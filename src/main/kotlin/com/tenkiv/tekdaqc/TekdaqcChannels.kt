package com.tenkiv.tekdaqc

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.QuantityMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData
import com.tenkiv.tekdaqc.communication.data_points.PWMInputData
import com.tenkiv.tekdaqc.communication.message.IDigitalChannelListener
import com.tenkiv.tekdaqc.communication.message.IPWMChannelListener
import com.tenkiv.tekdaqc.communication.message.IVoltageListener
import com.tenkiv.tekdaqc.hardware.AAnalogInput
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Gain
import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.hardware.AnalogInput_RevD
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.physikal.core.*
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.Units.HERTZ
import tec.uom.se.unit.Units.VOLT
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency
import com.tenkiv.tekdaqc.hardware.ATekdaqc.AnalogScale as Scale


class TekdaqcAnalogInput(val tekdaqc: TekdaqcBoard, val input: AAnalogInput) : AnalogInput(), IVoltageListener {
    override val broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<ElectricPotential>>()
    override val device: Device = tekdaqc
    override val hardwareNumber: Int = input.channelNumber

    override val isActivated: Boolean
        get() = input.isActivated

    val analogInputSwitchingTime = 4.nano.second

    override fun activate() { println("Trying activate");input.activate() }

    override fun deactivate() { println("Trying deactivate");input.deactivate() }

    init { input.addVoltageListener(this) }

    private var _buffer: Boolean = true

    override var buffer: Boolean
            get()=_buffer
            set(state: Boolean){
                _buffer = state
                if (state) { (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.ENABLED }
                else { (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.DISABLED }
            }

    private var _maxElectricPotential: ComparableQuantity<ElectricPotential> = 3.volt

    override var maxElectricPotential: ComparableQuantity<ElectricPotential>
        get() = _maxElectricPotential
        set(value) {_maxElectricPotential = value; recalculateState() }

    private var _maxAllowableError: ComparableQuantity<ElectricPotential> = 1.micro.volt

    override var maxAcceptableError: ComparableQuantity<ElectricPotential>
            get() = _maxAllowableError
            set(value) {_maxAllowableError = value; recalculateState() }

    fun recalculateState(){
        val voltageSettings = maxVoltageSettings(maxElectricPotential.tu(VOLT).toDouble())
        val rate = getFastestRateForAccuracy(
                voltageSettings.first,
                voltageSettings.second,
                maxAcceptableError,
                tekdaqc.lineFrequency)

        tekdaqc.tekdaqc.analogInputs[hardwareNumber]?.rate = rate
        tekdaqc.tekdaqc.analogInputs[hardwareNumber]?.gain = voltageSettings.first
        tekdaqc.tekdaqc.analogInputScale = voltageSettings.second

        //TODO Println Notice of Status Change or Jumper Requirements
    }

    fun maxVoltageSettings(voltage: Double): Pair<AAnalogInput.Gain,ATekdaqc.AnalogScale>{

        var gain: AAnalogInput.Gain = AAnalogInput.Gain.X2
        var scale: ATekdaqc.AnalogScale = ATekdaqc.AnalogScale.ANALOG_SCALE_400V

        when(true){
            (voltage > 400) -> {throw Exception("Voltage out of range")}
            (voltage <= 400 && voltage > 200) -> {
                gain = Gain.X1
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true }
            (voltage <= 200 && voltage > 100) -> {
                gain = Gain.X2
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true}
            (voltage <= 100 && voltage > 50) -> {
                gain = Gain.X4
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true}
            (voltage <= 50 && voltage > 25) -> {
                gain = Gain.X8
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true}
            (voltage <=50 && voltage > 12.5) -> {
                gain = Gain.X16
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true}
            (voltage <= 12.5 && voltage > 6.25) -> {
                gain = Gain.X32
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true}
            ((voltage <= 6.25 && voltage > 3.5) || (voltage <= 6.25 && tekdaqc.mandatory400Voltage)) -> {
                gain = Gain.X64
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true}
            (voltage <= 3.5 && voltage > 2.5 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X1
                scale = Scale.ANALOG_SCALE_5V}
            (voltage <= 2.5 && voltage > 1.25 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X2
                scale = Scale.ANALOG_SCALE_5V}
            (voltage <= 1.25 && voltage > .625 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X4
                scale = Scale.ANALOG_SCALE_5V}
            (voltage <= .625 && voltage > .3125 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X8
                scale = Scale.ANALOG_SCALE_5V}
            (voltage <= .3125 && voltage > .15625 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X16
                scale = Scale.ANALOG_SCALE_5V}
            (voltage <= .15625 && voltage > .078125 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X32
                scale = Scale.ANALOG_SCALE_5V}
            (voltage >= .078125 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X64
                scale = Scale.ANALOG_SCALE_5V}
        }
        return Pair(gain,scale)
    }

    override fun onVoltageDataReceived(input: AAnalogInput,
                                       value: ValueInstant<ComparableQuantity<ElectricPotential>>) {
        broadcastChannel.offer(DaqcQuantity.of(value.value).at(value.instant))
    }
}

class TekdaqcDigitalInput(val tekdaqc: TekdaqcBoard, val input: com.tenkiv.tekdaqc.hardware.DigitalInput) :
        DigitalInput(), IDigitalChannelListener, IPWMChannelListener {

    override val isActivated: Boolean
        get() = false

    override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcValue>>()
    override val device: Device = tekdaqc
    override val hardwareNumber: Int = input.channelNumber

    var isPercentOn: Boolean? = true

    private suspend fun rebroadcastToMain(value: ValueInstant<DaqcValue>){
        broadcastChannel.send(value)
    }

    override fun activate() { input.deactivatePWM(); input.activate() }

    override fun deactivate() { input.deactivatePWM(); input.deactivate() }

    init {
        input.addDigitalListener(this)
        input.addPWMListener(this)
        launch(DAQC_CONTEXT) { currentStateBroadcastChannel.consumeEach { rebroadcastToMain(it) } }
        launch(DAQC_CONTEXT) { pwmBroadcastChannel.consumeEach { rebroadcastToMain(it) } }
        launch(DAQC_CONTEXT) { transitionFrequencyBroadcastChannel.consumeEach { rebroadcastToMain(it) } }
        }

    override fun activateForCurrentState() {
        activate()
    }

    override fun activateForTransitionFrequency() {
        input.deactivate()
        isPercentOn = false
        input.activatePWM()
    }

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        //TODO Use Average Frequency
        input.deactivate()
        isPercentOn = true
        input.activatePWM()
    }

    override fun onDigitalDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput?, data: DigitalInputData) {
        runBlocking {
            when(data.state){
                true -> {currentStateBroadcastChannel.send(BinaryState.On.at(Instant.ofEpochMilli(data.timestamp)))}
                false -> {currentStateBroadcastChannel.send(BinaryState.Off.at(Instant.ofEpochMilli(data.timestamp)))}
            }
        }
    }

    override fun onPWMDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput, data: PWMInputData) {

            pwmBroadcastChannel.offer(ValueInstant.invoke(
                    DaqcQuantity.of(data.percetageOn.percent), Instant.ofEpochMilli(data.timestamp)))

            transitionFrequencyBroadcastChannel.offer(ValueInstant.invoke(
                    //TODO This isn't accurate need time stamp val to calculate Hertz
                    DaqcQuantity.of(data.totalTransitions.hertz), Instant.ofEpochMilli(data.timestamp)))
    }
}

class TekdaqcDigitalOutput(tekdaqc: TekdaqcBoard, val output: com.tenkiv.tekdaqc.hardware.DigitalOutput) :
        DigitalOutput() {

    override val broadcastChannel: ConflatedBroadcastChannel<DaqcValue> = ConflatedBroadcastChannel()

    override val pwmIsSimulated: Boolean = false
    override val transitionFrequencyIsSimulated: Boolean = false
    override val device: Device = tekdaqc
    override val hardwareNumber: Int = output.channelNumber

    private var frequencyJob: Job? = null

    override fun setOutput(setting: BinaryState) {
        frequencyJob?.cancel()
        when(setting){
            BinaryState.On -> { output.activate() }
            BinaryState.Off -> { output.deactivate() }
        }
        broadcastChannel.offer(setting)
    }

    override fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>) {
        frequencyJob?.cancel()
        output.setPulseWidthModulation(percent)
        broadcastChannel.offer(percent)
    }

    override fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>) {
        frequencyJob = launch(DAQC_CONTEXT){
            val cycleSpeec = ((freq tu HERTZ)/2).toLong()
            var isOn = false
            while (true){
                when {
                    (isOn)->{output.deactivate()}
                    (!isOn)->{output.activate()}
                }
                isOn = !isOn
                delay(cycleSpeec,TimeUnit.SECONDS)
            }
        }
        broadcastChannel.offer(freq)
    }
}