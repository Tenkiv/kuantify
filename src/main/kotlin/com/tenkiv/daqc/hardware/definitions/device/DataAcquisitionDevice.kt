package com.tenkiv.daqc.hardware.definitions.device

import com.tenkiv.Measurement
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.networking.SharingStatus

interface DataAcquisitionDevice: Device{

    val analogInputs: List<AnalogInput>

    val digitalInputs: List<DigitalInput>

    val hasAnalogInputs: Boolean

    val hasDigitalInputs: Boolean

    val sharedInputs: MutableMap<SharingStatus, Input<Measurement>>

}