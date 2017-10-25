package org.tenkiv.daqc

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

interface Updatable<out T> {

    val broadcastChannel: ConflatedBroadcastChannel<out T>

    fun openNewCoroutineListener(context: CoroutineContext = DefaultDispatcher, onUpdate: suspend (T) -> Unit): Job =
            launch(context) { broadcastChannel.consumeEach { onUpdate(it) } }

}