package org.tenkiv.daqc.hardware.definitions.device

import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.channel.AnalogOutput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.hardware.definitions.channel.Output
import org.tenkiv.daqc.networking.SharingStatus

interface ControlDevice : Device {

    val analogOutputs: List<AnalogOutput>

    val digitalOutputs: List<DigitalOutput>

    val hasAnalogOutputs: Boolean

    val hasDigitalOutputs: Boolean

    val sharedOutputs: MutableMap<SharingStatus, Output<DaqcValue>>

}