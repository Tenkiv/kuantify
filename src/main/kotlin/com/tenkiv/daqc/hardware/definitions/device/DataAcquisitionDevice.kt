package com.tenkiv.daqc.hardware.definitions.device

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.networking.SharingStatus

/**
 * Created by tenkiv on 4/7/17.
 */
interface DataAcquisitionDevice {

    val analogInputs: List<AnalogInput>

    val digitalInputs: List<DigitalInput>

    fun hasAnalogInputs(): Boolean

    fun hasDigitalInputs(): Boolean

    val sharedInputs: MutableMap<SharingStatus, Input<DaqcValue>>

}