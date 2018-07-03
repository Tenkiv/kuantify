package org.tenkiv.daqc.lib

import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

fun <E> BroadcastChannel<E>.openNewCoroutineListener(
    context: CoroutineContext,
    onReceive: suspend (E) -> Unit
) = launch(context) { this@openNewCoroutineListener.consumeEach { onReceive(it) } }
