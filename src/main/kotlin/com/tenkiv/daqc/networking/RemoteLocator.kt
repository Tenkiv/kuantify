package com.tenkiv.daqc.networking

import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 3/27/17.
 */
abstract class RemoteLocator<T: List<Device>>: Updatable<T> {

    abstract val activeDevices: T

    abstract fun search()

    abstract fun stop()

    override val context: CoroutineContext = newSingleThreadContext("RemoteDeviceLocator")

    override val broadcastChannel: BroadcastChannel<Updatable<T>> = ConflatedBroadcastChannel(this)

    protected var _value: T? = null

    override var latestValue: T?
        get() = _value
        set(value) { println("Value "+value); _value = value; launch(context){ broadcastChannel.send(this@RemoteLocator) } }

}