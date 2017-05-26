package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqc.DaqcValue
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 5/25/17.
 */
abstract class BasicUpdatable<T: DaqcValue>: Updatable<T> {

    var _value: T? = null

    override var value: T?
        get() = _value
        set(value) { _value = value; launch(context){ broadcastChannel.send(this@BasicUpdatable) } }

    override val context: CoroutineContext = newSingleThreadContext("Updatable Context")

    override val broadcastChannel: BroadcastChannel<Updatable<T>> = ConflatedBroadcastChannel()
}