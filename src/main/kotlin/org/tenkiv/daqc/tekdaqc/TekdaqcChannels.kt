package org.tenkiv.daqc.tekdaqc

import com.github.kittinunf.result.Result
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
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.*
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.hardware.definitions.device.Device
import org.tenkiv.daqc.hardware.inputs.ScAnalogSensor
import org.tenkiv.physikal.core.*
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import tec.uom.se.unit.Units.*
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency
import javax.measure.quantity.Temperature
import com.tenkiv.tekdaqc.hardware.ATekdaqc.AnalogScale as Scale


class TekdaqcAnalogInput(val tekdaqc: TekdaqcDevice, val input: AAnalogInput) : AnalogInput(), IVoltageListener {

    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()

    override val isActive: Boolean = input.isActivated
    override val broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<ElectricPotential>>()
    override val device: Device = tekdaqc
    override val hardwareNumber: Int = input.channelNumber

    override val sampleRate: ComparableQuantity<Frequency> = calculateSampleRate()

    private val analogInputSwitchingTime = 4.nano.second

    override fun activate() {
        input.activate()
    }

    override fun deactivate() {
        input.deactivate()
    }

    init {
        input.addVoltageListener(this)
    }

    @Volatile private var _buffer: Boolean = true

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
        return ((input.rate.rate.toDouble()) / ((switchingTime.toDoubleIn(SECOND)))).hertz
    }

    fun recalculateState() {
        val voltageSettings = maxVoltageSettings(maxElectricPotential.toDoubleIn(VOLT))
        println("Tekdaqc $tekdaqc ${tekdaqc.lineFrequency} ${tekdaqc.is400vManditory}")
        val rate = getFastestRateForAccuracy(
                voltageSettings.first,
                voltageSettings.second,
                maxAcceptableError,
                LineNoiseFrequency.AccountFor(60.hertz))

        tekdaqc.wrappedTekdaqc.analogInputs[hardwareNumber]?.rate = rate
        tekdaqc.wrappedTekdaqc.analogInputs[hardwareNumber]?.gain = voltageSettings.first

        if (tekdaqc.wrappedTekdaqc.analogScale != voltageSettings.second) {
            System.err.println("Tekdaqc Scale Updated. Jumper must be moved to ${voltageSettings.second}")
        }

        tekdaqc.wrappedTekdaqc.analogScale = voltageSettings.second

    }

    fun maxVoltageSettings(voltage: Double): Pair<AAnalogInput.Gain, ATekdaqc.AnalogScale> {

        val gain: AAnalogInput.Gain
        val scale: ATekdaqc.AnalogScale

        when {
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
            (voltage <= .078125 && !tekdaqc.mandatory400Voltage) -> {
                gain = Gain.X64
                scale = Scale.ANALOG_SCALE_5V
            }
            else -> {
                gain = AAnalogInput.Gain.X2
                scale = ATekdaqc.AnalogScale.ANALOG_SCALE_400V
            }
        }
        println("Decided on G:$gain and $scale from $voltage and manVolt: ${tekdaqc.mandatory400Voltage}")
        return Pair(gain, scale)
    }

    override fun onVoltageDataReceived(input: AAnalogInput,
                                       value: ValueInstant<ComparableQuantity<ElectricPotential>>) {
        broadcastChannel.offer(DaqcQuantity.of(value.value).at(value.instant))
    }
}

class TekdaqcDigitalInput(tekdaqc: TekdaqcDevice, val input: com.tenkiv.tekdaqc.hardware.DigitalInput) :
        DigitalInput(), IDigitalChannelListener, IPWMChannelListener {

    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()

    override val pwmIsSimulated: Boolean = false
    override val transitionFrequencyIsSimulated: Boolean = false

    override val device: Device = tekdaqc
    override val hardwareNumber: Int = input.channelNumber
    private var currentState = DigitalStatus.DEACTIVATED

    override val isActiveForBinaryState: Boolean = (currentState == DigitalStatus.ACTIVATED_STATE)
    override val isActiveForPwm: Boolean = (currentState == DigitalStatus.ACTIVATED_PWM)
    override val isActiveForTransitionFrequency: Boolean = (currentState == DigitalStatus.ACTIVATED_FREQUENCY)

    override fun activate() {
        /*input.deactivatePWM();*/ input.activate()
    }

    override fun deactivate() {
        /*input.deactivatePWM();*/ input.deactivate()
    }

    init {
        input.addDigitalListener(this)
        input.addPWMListener(this)
    }

    override fun activateForCurrentState() {
        activate(); currentState = DigitalStatus.ACTIVATED_STATE
    }

    override fun activateForTransitionFrequency(avgFrequency: DaqcQuantity<Frequency>) {
        input.deactivate()
        currentState = DigitalStatus.ACTIVATED_STATE; currentState = DigitalStatus.ACTIVATED_FREQUENCY
        //input.activatePWM()
    }

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        input.deactivate()
        currentState = DigitalStatus.ACTIVATED_PWM
        //input.activatePWM()
    }

    override fun onDigitalDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput?, data: DigitalInputData) {

        when (data.state) {
            true -> {
                _binaryStateBroadcastChannel.offer(BinaryState.On.at(Instant.ofEpochMilli(data.timestamp)))
            }
            false -> {
                _binaryStateBroadcastChannel.offer(BinaryState.Off.at(Instant.ofEpochMilli(data.timestamp)))
            }
        }

    }

    override fun onPWMDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput, data: PWMInputData) {

        _pwmBroadcastChannel.offer(ValueInstant.invoke(
                DaqcQuantity.of(data.percetageOn.percent), Instant.ofEpochMilli(data.timestamp)))

        _transitionFrequencyBroadcastChannel.offer(ValueInstant.invoke(
                //TODO This isn't accurate need time stamp val to calculate Hertz
                DaqcQuantity.of(data.totalTransitions.hertz), Instant.ofEpochMilli(data.timestamp)))
    }
}

class TekdaqcDigitalOutput(tekdaqc: TekdaqcDevice, val output: com.tenkiv.tekdaqc.hardware.DigitalOutput) :
        DigitalOutput() {

    private var currentState = DigitalStatus.DEACTIVATED
    override val pwmIsSimulated: Boolean = false
    override val transitionFrequencyIsSimulated: Boolean = false
    override val device: Device = tekdaqc
    override val hardwareNumber: Int = output.channelNumber
    override val isActiveForBinaryState: Boolean = (currentState == DigitalStatus.ACTIVATED_STATE)
    override val isActiveForPwm: Boolean = (currentState == DigitalStatus.ACTIVATED_PWM)
    override val isActiveForTransitionFrequency: Boolean = (currentState == DigitalStatus.ACTIVATED_FREQUENCY)

    private var frequencyJob: Job? = null

    override fun setOutput(setting: BinaryState) {
        frequencyJob?.cancel()
        currentState = when (setting) {
            BinaryState.On -> {
                output.activate(); DigitalStatus.ACTIVATED_STATE
            }
            BinaryState.Off -> {
                output.deactivate(); DigitalStatus.DEACTIVATED
            }
        }
        _binaryStateBroadcastChannel.offer(setting.at(Instant.now()))
    }

    override fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>) {
        frequencyJob?.cancel()
        output.setPulseWidthModulation(percent)
        currentState = DigitalStatus.ACTIVATED_PWM
        _pwmBroadcastChannel.offer(percent.at(Instant.now()))
    }

    override fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>) {
        currentState = DigitalStatus.ACTIVATED_FREQUENCY
        frequencyJob = launch(daqcThreadContext) {
            val cycleSpeed = ((freq) / 2).toLongIn(HERTZ)
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
                delay(cycleSpeed, TimeUnit.SECONDS)
            }
        }
        _transitionFrequencyBroadcastChannel.offer(freq.at(Instant.now()))
    }
}

class TekdaqcTemperatureReference(analogInput: TekdaqcAnalogInput) : ScAnalogSensor<Temperature>(analogInput,
        10.micro.volt,
        3.volt) {

    override fun convertInput(ep: ComparableQuantity<ElectricPotential>): Result<DaqcQuantity<Temperature>, Exception> {
        return Result.Companion.of(DaqcQuantity<Temperature>(Quantities.getQuantity(ep.toDoubleIn(VOLT) / .01, CELSIUS)))
    }

}