package com.tenkiv.tekdaqc

import com.tenkiv.AccuracySetting
import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.*
import com.tenkiv.daqc.hardware.definitions.HardwareType
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

/**
 * Created by tenkiv on 5/26/17.
 */

class TekdaqcAnalogInput(val tekdaqc: TekdaqcBoard, val input: AAnalogInput) : AnalogInput(), IVoltageListener {
    override val broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<ElectricPotential>>()
    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.ANALOG_INPUT
    override val hardwareNumber: Int = input.channelNumber

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

    var _accuracy: AccuracySetting = AccuracySetting(AnalogAccuracy.DECIVOLT, 2.5.volt)

    override var accuracy: AccuracySetting
            get() = _accuracy
            set(value) {
                val requiredVoltageSettings = maxVoltageSettings(value.second.tu(VOLT).toDouble())

                var rate: AAnalogInput.Rate = AAnalogInput.Rate.SPS_2_5

                when (value.first) {
                // This may not be correct. Look over with Bill before release.
                    AnalogAccuracy.DECIVOLT -> rate = AAnalogInput.Rate.SPS_1000
                    AnalogAccuracy.CENTIVOLT -> rate = AAnalogInput.Rate.SPS_50 //60 DEPENDING ON LOCATION
                    AnalogAccuracy.MILLIVOLT -> rate = AAnalogInput.Rate.SPS_15
                    AnalogAccuracy.DECIMILLIVOLT -> rate = AAnalogInput.Rate.SPS_10
                    AnalogAccuracy.CENTIMILLIVOLT -> rate = AAnalogInput.Rate.SPS_5
                    AnalogAccuracy.MICROVOLT -> rate = AAnalogInput.Rate.SPS_2_5
                }

                tekdaqc.tekdaqc.analogInputs[hardwareNumber]?.rate = rate
                tekdaqc.tekdaqc.analogInputs[hardwareNumber]?.gain = requiredVoltageSettings.first
                tekdaqc.tekdaqc.analogInputScale = requiredVoltageSettings.second
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
            ((voltage <= 6.25 && voltage > 5) || (voltage <= 6.25 && tekdaqc.mandatory400Voltage)) -> {
                gain = Gain.X64
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true}
            (voltage <= 5 && voltage > 2.5 && !tekdaqc.mandatory400Voltage) -> {
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
        launch(DAQC_CONTEXT) { broadcastChannel.send(DaqcQuantity.of(value.value).at(value.instant)) }
    }
}

class TekdaqcDigitalInput(val tekdaqc: TekdaqcBoard, val input: com.tenkiv.tekdaqc.hardware.DigitalInput) :
        DigitalInput(), IDigitalChannelListener, IPWMChannelListener {

    override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcValue>>()
    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.DIGITAL_INPUT
    override val hardwareNumber: Int = input.channelNumber

    var isPercentOn: Boolean? = true

    private fun rebroadcastToMain(value: ValueInstant<DaqcValue>){
        launch(DAQC_CONTEXT) { broadcastChannel.send(value) }
    }

    override fun activate() { input.deactivatePWM(); input.activate() }

    override fun deactivate() { input.deactivatePWM(); input.deactivate() }

    init {
        input.addDigitalListener(this)
        input.addPWMListener(this)
        launch(DAQC_CONTEXT) { currentStateBroadcastChannel.consumeEach { rebroadcastToMain(it) } }

        }

    override fun activateForCurrentState() {
        activate()
    }

    override fun activateForTransitionFrequency() {
        input.deactivate()
        isPercentOn = false
        input.activatePWM()
    }

    override fun activateForPwm(avgFrequency: ComparableQuantity<Frequency>) {
        //TODO Use Average Frequency
        input.deactivate()
        isPercentOn = true
        input.activatePWM()
    }

    override fun onDigitalDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput?, data: DigitalInputData) {
        launch(DAQC_CONTEXT) {
            when(data.state){
                true -> {
                    currentStateBroadcastChannel.send(BinaryState.On.at(Instant.ofEpochMilli(data.timestamp)))
                }
                false -> {
                    currentStateBroadcastChannel.send(BinaryState.Off.at(Instant.ofEpochMilli(data.timestamp)))
                }
            }
        }
    }

    override fun onPWMDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput, data: PWMInputData) {
        launch(DAQC_CONTEXT) {

            pwmBroadcastChannel.send(ValueInstant.invoke(
                    DaqcQuantity.of(data.percetageOn.percent), Instant.ofEpochMilli(data.timestamp)))

            transitionFrequencyBroadcastChannel.send(ValueInstant.invoke(
                    //TODO This isn't accurate need time stamp val to calculate Hertz
                    DaqcQuantity.of(data.totalTransitions.hertz), Instant.ofEpochMilli(data.timestamp)))
        }
    }
}

class TekdaqcDigitalOutput(val tekdaqc: TekdaqcBoard, val output: com.tenkiv.tekdaqc.hardware.DigitalOutput) :
        DigitalOutput() {

    override val pwmIsSimulated: Boolean = true
    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.DIGITAL_OUTPUT
    override val hardwareNumber: Int = output.channelNumber

    var frequencyJob: Job? = null

    override fun activate() {
        frequencyJob?.cancel()
        output.activate()
    }

    override fun deactivate() {
        frequencyJob?.cancel()
        output.deactivate()
    }

    override fun pulseWidthModulate(percent: ComparableQuantity<Dimensionless>) {
        frequencyJob?.cancel()
        output.setPulseWidthModulation(percent)
    }

    override fun setTransitionFrequency(freq: ComparableQuantity<Frequency>) {
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
    }
    override val broadcastChannel = ConflatedBroadcastChannel<BinaryState>()
}