/*
 * Copyright 2018 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.kuantify.lib

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext

/**
 * Opens a coroutine listener and consumes updates with the given function.
 *
 * @param context The context for the channel to be opened.
 * @param onReceive The function to be executed when an update is received.
 */
fun <E> BroadcastChannel<E>.openNewCoroutineListener(
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
    onReceive: suspend (E) -> Unit
) = scope.launch(context) { this@openNewCoroutineListener.consumeEach { onReceive(it) } }

/**
 * Opens a coroutine listener and consumes updates with the given function, also returns the new channel.
 *
 * @param context The context for the channel to be opened.
 * @param onReceive The function to be executed when an update is received.
 * @return The opened [ReceiveChannel].
 */
fun <T> BroadcastChannel<T>.consumeAndReturn(
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
    onReceive: suspend (T) -> Unit
): ReceiveChannel<T> {
    val subChannel = openSubscription()
    scope.launch(context) {
        subChannel.consume { for (x in this) onReceive(x) }
    }
    return subChannel
}