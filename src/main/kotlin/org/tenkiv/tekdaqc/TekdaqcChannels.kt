package org.tenkiv.tekdaqc

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
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.DigitalStatus
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.hardware.definitions.device.Device
import org.tenkiv.daqcThreadContext
import org.tenkiv.physikal.core.*
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.Units.*
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency
import com.tenkiv.tekdaqc.hardware.ATekdaqc.AnalogScale as Scale


class TekdaqcAnalogInput(val tekdaqc: TekdaqcBoard, val input: AAnalogInput) : AnalogInput(), IVoltageListener {
    override val isActive: Boolean = input.isActivated
    override val broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<ElectricPotential>>()
    override val device: Device = tekdaqc
    override val hardwareNumber: Int = input.channelNumber

    override val sampleRate: ComparableQuantity<Frequency> = calculateSampleRate()

    val analogInputSwitchingTime = 4.nano.second

    override fun activate() {
        input.activate()
    }

    override fun deactivate() {
        input.deactivate()
    }

    init {
        input.addVoltageListener(this)
    }

    private var _buffer: Boolean = true

    override var buffer: Boolean
        get() = _buffer
        set(state) {
            _buffer = state
            if (state) {
                (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.ENABLED
            } else {
                (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.DISABLED
            }
        }

    private var _maxElectricPotential: ComparableQuantity<ElectricPotential> = 3.volt

    override var maxElectricPotential: ComparableQuantity<ElectricPotential>
        get() = _maxElectricPotential
        set(value) {
            _maxElectricPotential = value; recalculateState()
        }

    private var _maxAllowableError: ComparableQuantity<ElectricPotential> = 1.micro.volt

    override var maxAcceptableError: ComparableQuantity<ElectricPotential>
        get() = _maxAllowableError
        set(value) {
            _maxAllowableError = value; recalculateState()
        }

    private fun calculateSampleRate(): ComparableQuantity<Frequency> {
        if (!isActive) {
            return 0.hertz
        }
        val activatedInputs = tekdaqc.analogInputs.filter { isActive }.size
        val switchingTime = if (activatedInputs == 1) {
            0.second
        } else {
            analogInputSwitchingTime * activatedInputs
        }
        return ((input.rate.rate.toDouble()) / ((switchingTime tu SECOND).toDouble())).hertz
    }

    fun recalculateState() {
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

    fun maxVoltageSettings(voltage: Double): Pair<AAnalogInput.Gain, ATekdaqc.AnalogScale> {

        var gain: AAnalogInput.Gain = AAnalogInput.Gain.X2
        var scale: ATekdaqc.AnalogScale = ATekdaqc.AnalogScale.ANALOG_SCALE_400V

        when (true) {
            (voltage > 400) -> {
                throw Exception("Voltage out of range")
            }
            (voltage <= 400 && voltage > 200) -> {
                gain = Gain.X1
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true
            }
            (voltage <= 200 && voltage > 100) -> {
                gain = Gain.X2
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true
            }
            (voltage <= 100 && voltage > 50) -> {
                gain = Gain.X4
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true
            }
            (voltage <= 50 && voltage > 25) -> {
                gain = Gain.X8
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true
            }
            (voltage <= 50 && voltage > 12.5) -> {
                gain = Gain.X16
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true
            }
            (voltage <= 12.5 && voltage > 6.25) -> {
                gain = Gain.X32
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true
            }
            ((voltage <= 6.25 && voltage > 3.5) || (voltage <= 6.25 && tekdaqc.mandatory400Voltage)) -> {
                gain = Gain.X64
                scale = Scale.ANALOG_SCALE_400V
                tekdaqc.mandatory400Voltage = true
            }
            (voltage <= 3.5 && voltage > 2.5 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X1
                scale = Scale.ANALOG_SCALE_5V
            }
            (voltage <= 2.5 && voltage > 1.25 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X2
                scale = Scale.ANALOG_SCALE_5V
            }
            (voltage <= 1.25 && voltage > .625 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X4
                scale = Scale.ANALOG_SCALE_5V
            }
            (voltage <= .625 && voltage > .3125 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X8
                scale = Scale.ANALOG_SCALE_5V
            }
            (voltage <= .3125 && voltage > .15625 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X16
                scale = Scale.ANALOG_SCALE_5V
            }
            (voltage <= .15625 && voltage > .078125 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X32
                scale = Scale.ANALOG_SCALE_5V
            }
            (voltage >= .078125 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X64
                scale = Scale.ANALOG_SCALE_5V
            }
        }
        return Pair(gain, scale)
    }

    override fun onVoltageDataReceived(input: AAnalogInput,
                                       value: ValueInstant<ComparableQuantity<ElectricPotential>>) {
        broadcastChannel.offer(DaqcQuantity.of(value.value).at(value.instant))
    }
}

class TekdaqcDigitalInput(val tekdaqc: TekdaqcBoard, val input: com.tenkiv.tekdaqc.hardware.DigitalInput) :
        DigitalInput(), IDigitalChannelListener, IPWMChannelListener {

    override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcValue>>()
    override val device: Device = tekdaqc
    override val hardwareNumber: Int = input.channelNumber
    private var currentState = DigitalStatus.DEACTIVATED

    override val isActiveForBinaryState: Boolean = (currentState == DigitalStatus.ACTIVATED_STATE)
    override val isActiveForPwm: Boolean = (currentState == DigitalStatus.ACTIVATED_PWM)
    override val isActiveForTransitionFrequency: Boolean = (currentState == DigitalStatus.ACTIVATED_FREQUENCY)

    private suspend fun rebroadcastToMain(value: ValueInstant<DaqcValue>) {
        broadcastChannel.send(value)
    }

    override fun activate() {
        input.deactivatePWM(); input.activate()
    }

    override fun deactivate() {
        input.deactivatePWM(); input.deactivate()
    }

    init {
        input.addDigitalListener(this)
        input.addPWMListener(this)
        launch(daqcThreadContext) { currentStateBroadcastChannel.consumeEach { rebroadcastToMain(it) } }
        launch(daqcThreadContext) { pwmBroadcastChannel.consumeEach { rebroadcastToMain(it) } }
        launch(daqcThreadContext) { transitionFrequencyBroadcastChannel.consumeEach { rebroadcastToMain(it) } }
    }

    override fun activateForCurrentState() {
        activate(); currentState = DigitalStatus.ACTIVATED_STATE
    }

    override fun activateForTransitionFrequency() {
        input.deactivate()
        currentState = DigitalStatus.ACTIVATED_STATE; currentState = DigitalStatus.ACTIVATED_FREQUENCY
        input.activatePWM()
    }

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        input.deactivate()
        currentState = DigitalStatus.ACTIVATED_PWM
        input.activatePWM()
    }

    override fun onDigitalDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput?, data: DigitalInputData) {
        runBlocking {
            when (data.state) {
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

        pwmBroadcastChannel.offer(ValueInstant.invoke(
                DaqcQuantity.of(data.percetageOn.percent), Instant.ofEpochMilli(data.timestamp)))

        transitionFrequencyBroadcastChannel.offer(ValueInstant.invoke(
                //TODO This isn't accurate need time stamp val to calculate Hertz
                DaqcQuantity.of(data.totalTransitions.hertz), Instant.ofEpochMilli(data.timestamp)))
    }
}

class TekdaqcDigitalOutput(tekdaqc: TekdaqcBoard, val output: com.tenkiv.tekdaqc.hardware.DigitalOutput) :
        DigitalOutput() {

    override val broadcastChannel: ConflatedBroadcastChannel<ValueInstant<DaqcValue>> = ConflatedBroadcastChannel()
    override val pwmIsSimulated: Boolean = false
    override val transitionFrequencyIsSimulated: Boolean = false
    override val device: Device = tekdaqc
    override val hardwareNumber: Int = output.channelNumber
    private var currentState = DigitalStatus.DEACTIVATED
    override val isActiveForBinaryState: Boolean = (currentState == DigitalStatus.ACTIVATED_STATE)
    override val isActiveForPwm: Boolean = (currentState == DigitalStatus.ACTIVATED_PWM)
    override val isActiveForTransitionFrequency: Boolean = (currentState == DigitalStatus.ACTIVATED_FREQUENCY)

    private var frequencyJob: Job? = null

    override fun setOutput(setting: BinaryState) {
        frequencyJob?.cancel()
        when (setting) {
            BinaryState.On -> {
                output.activate(); currentState = DigitalStatus.ACTIVATED_STATE
            }
            BinaryState.Off -> {
                output.deactivate(); currentState = DigitalStatus.DEACTIVATED
            }
        }
        broadcastChannel.offer(setting.at(Instant.now()))
    }

    override fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>) {
        frequencyJob?.cancel()
        output.setPulseWidthModulation(percent)
        currentState = DigitalStatus.ACTIVATED_PWM
        broadcastChannel.offer(percent.at(Instant.now()))
    }

    override fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>) {
        currentState = DigitalStatus.ACTIVATED_FREQUENCY
        frequencyJob = launch(daqcThreadContext) {
            val cycleSpeec = ((freq tu HERTZ) / 2).toLong()
            var isOn = false
            while (true) {
                when {
                    (isOn) -> {
                        output.deactivate()
                    }
                    (!isOn) -> {
                        output.activate()
                    }
                }
                isOn = !isOn
                delay(cycleSpeec, TimeUnit.SECONDS)
            }
        }
        broadcastChannel.offer(freq.at(Instant.now()))
    }
}