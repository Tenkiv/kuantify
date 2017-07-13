package org.tenkiv.daqc.hardware.definitions

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import org.tenkiv.daqcThreadContext
import kotlin.coroutines.experimental.CoroutineContext

interface Updatable<T> {

    val broadcastChannel: ConflatedBroadcastChannel<T>

    fun openNewCoroutineListener(context: CoroutineContext = daqcThreadContext, onUpdate: suspend (T) -> Unit): Job =
            launch(context) { broadcastChannel.consumeEach { onUpdate(it) } }

}