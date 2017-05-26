package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 4/11/17.
 */
interface Updatable<T: DaqcValue> {

    val context: CoroutineContext

    val broadcastChannel: BroadcastChannel<Updatable<T>>

    var value: T?

}