package com.tenkiv.tekdaqc

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.*
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData
import com.tenkiv.tekdaqc.communication.message.IDigitalChannelListener
import com.tenkiv.tekdaqc.communication.message.IVoltageListener
import com.tenkiv.tekdaqc.hardware.AAnalogInput
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Gain as Gain
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Rate as Rate
import com.tenkiv.tekdaqc.hardware.ATekdaqc.AnalogScale as Scale
import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.hardware.AnalogInput_RevD
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import org.tenkiv.physikal.core.nano
import org.tenkiv.physikal.core.second
import org.tenkiv.physikal.core.toDouble
import org.tenkiv.physikal.core.tu
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.Units.VOLT
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 5/26/17.
 */

class TekdaqcAnalogInput(val tekdaqc: TekdaqcBoard, val input: AAnalogInput) : AnalogInput(), IVoltageListener {
    override val broadcastChannel = ConflatedBroadcastChannel<DaqcQuantity<ElectricPotential>>()
    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.ANALOG_INPUT
    override val hardwareNumber: Int = input.channelNumber

    val analogInputSwitchingTime = 4.nano.second

    override fun activate() { println("Trying activate");input.activate() }

    override fun deactivate() { println("Trying deactivate");input.deactivate() }

    init { input.addVoltageListener(this) }

    override fun setBuffer(state: Boolean) {
        if (state) {
            (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.ENABLED
        } else {
            (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.DISABLED
        }
    }

    override fun setAccuracy(accuracy: AnalogAccuracy?, maxVoltage: ComparableQuantity<ElectricPotential>) {

        val requiredVoltageSettings = maxVoltageSettings(maxVoltage.tu(VOLT).toDouble())

        var rate: AAnalogInput.Rate = AAnalogInput.Rate.SPS_2_5

        when (accuracy) {
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
        launch(DAQC_CONTEXT) { broadcastChannel.send(DaqcQuantity.of(value.value)) }
    }
}

class TekdaqcDigitalInput(val tekdaqc: TekdaqcBoard, val input: com.tenkiv.tekdaqc.hardware.DigitalInput) :
        DigitalInput(), IDigitalChannelListener {
    override val broadcastChannel = ConflatedBroadcastChannel<BinaryState>()
    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.DIGITAL_INPUT
    override val hardwareNumber: Int = input.channelNumber
    override val canReadPulseWidthModulation: Boolean = true

    override fun activate() { input.activate() }

    override fun deactivate() { input.deactivate() }

    init { input.addDigitalListener(this) }

    override fun onDigitalDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput?, data: DigitalInputData) {
        launch(DAQC_CONTEXT) {
            when(data.state){
                true -> {broadcastChannel.send(BinaryState.On)}
                false -> {broadcastChannel.send(BinaryState.Off)}
            }
        }
    }
}

class TekdaqcDigitalOutput(val tekdaqc: TekdaqcBoard, val output: com.tenkiv.tekdaqc.hardware.DigitalOutput) :
        DigitalOutput() {

    override val canPulseWidthModulate: Boolean = true
    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.DIGITAL_OUTPUT
    override val hardwareNumber: Int = output.channelNumber
    override fun activate() {
        output.activate()
    }

    override fun deactivate() {
        output.deactivate()
    }

    override fun pulseWidthModulate(dutyCycle: Int) {
        output.setPulseWidthModulation(dutyCycle)
    }

    override val broadcastChannel = ConflatedBroadcastChannel<BinaryState>()



}