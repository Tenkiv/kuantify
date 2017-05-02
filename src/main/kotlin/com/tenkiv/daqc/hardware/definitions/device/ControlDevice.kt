package com.tenkiv.daqc.hardware.definitions.device

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.Trigger
import com.tenkiv.daqc.hardware.definitions.channel.AnalogOutput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import com.tenkiv.daqc.hardware.definitions.channel.Output
import com.tenkiv.daqc.networking.SharingStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by tenkiv on 4/7/17.
 */
abstract class ControlDevice: Device {

    val getAnalogOutputs: List<AnalogOutput> = CopyOnWriteArrayList()

    val getDigitalOutputs: List<DigitalOutput> = CopyOnWriteArrayList()

    protected val sharedOutputs: MutableMap<SharingStatus,Output<DaqcValue>> = ConcurrentHashMap()

    abstract fun hasAnalogOutputs(): Boolean

    abstract fun hasDigitalOutputs(): Boolean


}