package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqc.DaqcValue
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 4/11/17.
 */
interface Updatable<T> {

    val context: CoroutineContext

    val broadcastChannel: BroadcastChannel<Updatable<T>>

    var latestValue: T?

}