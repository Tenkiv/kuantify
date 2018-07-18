package org.tenkiv.daqc

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlin.coroutines.experimental.CoroutineContext

interface Updatable<out T> {

    val broadcastChannel: ConflatedBroadcastChannel<out T>

    val valueOrNull get() = broadcastChannel.valueOrNull

    val value: Deferred<T>
        get() = async {
            broadcastChannel.valueOrNull ?: broadcastChannel.openSubscription().receive()
        }

    suspend fun getValue(): T = broadcastChannel.valueOrNull ?: broadcastChannel.openSubscription().receive()

    fun openNewCoroutineListener(context: CoroutineContext = DefaultDispatcher, onUpdate: suspend (T) -> Unit): Job =
        launch(context) { broadcastChannel.consumeEach { onUpdate(it) } }
}