package com.tenkiv.daqc.networking

import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.device.Device
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

/**
 * Created by tenkiv on 3/27/17.
 */
abstract class RemoteLocator {

    val activeDevices: MutableList<Device> = CopyOnWriteArrayList()

    abstract fun search()

    abstract fun stop()

}