package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqcThreadContext
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

interface Updatable<T> {

    val broadcastChannel: ConflatedBroadcastChannel<T>

    fun openNewCoroutineListener(context: CoroutineContext = daqcThreadContext, onUpdate: suspend (T) -> Unit) =
            launch(context) { broadcastChannel.consumeEach { onUpdate(it) } }

}