package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.DAQC_CONTEXT
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 4/11/17.
 */
interface Updatable<T> {

    val broadcastChannel: ConflatedBroadcastChannel<T>

    fun openNewCoroutineListener(context: CoroutineContext = DAQC_CONTEXT, onUpdate: suspend (T) -> Unit) =
            launch(context) { broadcastChannel.open().consumeEach { onUpdate(it) } }

}