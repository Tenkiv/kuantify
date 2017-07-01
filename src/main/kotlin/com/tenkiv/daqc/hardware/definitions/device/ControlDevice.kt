package com.tenkiv.daqc.hardware.definitions.device

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.channel.AnalogOutput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import com.tenkiv.daqc.hardware.definitions.channel.Output
import com.tenkiv.daqc.networking.SharingStatus

interface ControlDevice : Device {

    val analogOutputs: List<AnalogOutput>

    val digitalOutputs: List<DigitalOutput>

    fun hasAnalogOutputs(): Boolean

    fun hasDigitalOutputs(): Boolean

    val sharedOutputs: MutableMap<SharingStatus, Output<DaqcValue>>

}