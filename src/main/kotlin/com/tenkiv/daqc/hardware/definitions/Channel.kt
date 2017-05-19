package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.daqc.recording.InMemoryRecorder
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.ArrayList
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Created by tenkiv on 3/18/17.
 */
interface Channel<T: DaqcValue>: Updatable<T> {

    val device: Device

    val hardwareType: HardwareType

    val hardwareNumber: Int

    fun activate()

    fun deactivate()

}