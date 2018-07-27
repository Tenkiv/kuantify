package org.tenkiv.daqc.lib

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

fun <E> BroadcastChannel<E>.openNewCoroutineListener(
    context: CoroutineContext,
    onReceive: suspend (E) -> Unit
) = launch(context) { this@openNewCoroutineListener.consumeEach { onReceive(it) } }

fun <T> BroadcastChannel<T>.consumeAndReturn(
    context: CoroutineContext = DefaultDispatcher,
    action: suspend (T) -> Unit
): ReceiveChannel<T> {
    val subChannel = openSubscription()
    launch(context) {
        subChannel.consume { for (x in this) action(x) }
    }
    return subChannel
}