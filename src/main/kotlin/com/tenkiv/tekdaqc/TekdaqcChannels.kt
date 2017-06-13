package com.tenkiv.tekdaqc

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.AnalogAccuracy
import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData
import com.tenkiv.tekdaqc.communication.message.IDigitalChannelListener
import com.tenkiv.tekdaqc.communication.message.IVoltageListener
import com.tenkiv.tekdaqc.hardware.AAnalogInput
import com.tenkiv.tekdaqc.hardware.AnalogInput_RevD
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 5/26/17.
 */

class TekdaqcAnalogInput(val tekdaqc: TekdaqcBoard, val input: AAnalogInput) : AnalogInput(), IVoltageListener {
    override val broadcastChannel = ConflatedBroadcastChannel<DaqcQuantity<ElectricPotential>>()
    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.ANALOG_INPUT
    override val hardwareNumber: Int = input.channelNumber
    override fun activate() {
        println("Trying activate");input.activate()
    }

    override fun deactivate() {
        println("Trying deactivate");input.deactivate()
    }

    init {
        input.addVoltageListener(this)
    }

    override fun setBuffer(state: Boolean) {
        if (state) {
            (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.ENABLED
        } else {
            (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.DISABLED
        }
    }

    override fun setAccuracy(accuracy: AnalogAccuracy) {
        when (accuracy) {
            AnalogAccuracy.ONE_TENTH_VOLT -> TODO()
            AnalogAccuracy.ONE_HUNDREDTH_VOLT -> TODO()
            AnalogAccuracy.ONE_THOUSANDTH_VOLT -> TODO()
            AnalogAccuracy.ONE_TEN_THOUSANDTH_VOLT -> TODO()
            AnalogAccuracy.ONE_HUNDRED_THOUSANDTH_VOLT -> TODO()
            AnalogAccuracy.ONE_MILLIONTH_VOLT -> TODO()
        }
    }

    override fun onVoltageDataReceived(input: AAnalogInput, value: ValueInstant<ComparableQuantity<ElectricPotential>>) {
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
    override fun activate() {
        input.activate()
    }

    override fun deactivate() {
        input.deactivate()
    }

    init {
        input.addDigitalListener(this)
    }

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