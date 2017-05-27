package com.tenkiv.tekdaqc

import com.tenkiv.daqc.AnalogAccuracy
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData
import com.tenkiv.tekdaqc.communication.message.IDigitalChannelListener
import com.tenkiv.tekdaqc.communication.message.IVoltageListener
import com.tenkiv.tekdaqc.communication.message.PLACEHOLDERMEASUREMENT
import com.tenkiv.tekdaqc.hardware.AAnalogInput
import com.tenkiv.tekdaqc.hardware.AnalogInput_RevD
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 5/26/17.
 */

class TekdaqcAnalogInput(val tekdaqc: TekdaqcBoard, val input: AAnalogInput): AnalogInput(), IVoltageListener {

    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.ANALOG_INPUT
    override val hardwareNumber: Int = input.channelNumber
    override fun activate() { input.activate() }
    override fun deactivate() { input.deactivate() }


    override fun setBuffer(state: Boolean) {
        if(state){
            (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.ENABLED
        }else{
            (input as? AnalogInput_RevD)?.bufferState = AnalogInput_RevD.BufferState.DISABLED
        }
    }

    override fun setAccuracy(accuracy: AnalogAccuracy) {
        when(accuracy){
            AnalogAccuracy.FASTEST -> { input.rate = AAnalogInput.Rate.SPS_1000 }
            AnalogAccuracy.BALANCED -> { input.rate = AAnalogInput.Rate.SPS_100 }
            AnalogAccuracy.PRECISE -> { input.rate = AAnalogInput.Rate.SPS_2_5 }
        }
    }

    override fun onVoltageDataReceived(input: AAnalogInput?, measurement: PLACEHOLDERMEASUREMENT<ElectricPotential>) {
        value = null
    }
}

class TekdaqcDigitalInput(val tekdaqc: TekdaqcBoard, val input: com.tenkiv.tekdaqc.hardware.DigitalInput): DigitalInput(), IDigitalChannelListener {

    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.DIGITAL_INPUT
    override val hardwareNumber: Int = input.channelNumber
    override val canReadPulseWidthModulation: Boolean = true
    override fun activate() { input.activate() }
    override fun deactivate() { input.deactivate() }

    override fun onDigitalDataReceived(input: com.tenkiv.tekdaqc.hardware.DigitalInput?, data: DigitalInputData) {
        value = DaqcValue.Boolean(data.state)
    }
}

class TekdaqcDigitalOutput(val tekdaqc: TekdaqcBoard, val output: com.tenkiv.tekdaqc.hardware.DigitalOutput): DigitalOutput() {

    override val canPulseWidthModulate: Boolean = true
    override val device: Device = tekdaqc
    override val hardwareType: HardwareType = HardwareType.DIGITAL_OUTPUT
    override val hardwareNumber: Int = output.channelNumber
    override fun activate() { output.activate() }
    override fun deactivate() { output.deactivate() }

    override fun pulseWidthModulate(dutyCycle: Float) {

    }
}