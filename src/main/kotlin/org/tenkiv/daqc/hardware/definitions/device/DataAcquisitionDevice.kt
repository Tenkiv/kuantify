package org.tenkiv.daqc.hardware.definitions.device

import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.Input
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.networking.SharingStatus

interface DataAcquisitionDevice : Device {

    val analogInputs: List<AnalogInput>

    val digitalInputs: List<DigitalInput>

    val hasAnalogInputs: Boolean

    val hasDigitalInputs: Boolean

    val sharedInputs: MutableMap<SharingStatus, Input<DaqcValue>>

}