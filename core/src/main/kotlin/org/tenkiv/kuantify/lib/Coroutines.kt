/*
 * Copyright 2019 Tenkiv, Inc.
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
 *
 */

package org.tenkiv.kuantify.lib

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.*

//TODO: I think this function should be removed. It is only used in trigger and I think there is a more idiomatic way.
/**
 * Opens a coroutine listener and consumes updates with the given function, also returns the new channel.
 *
 * @param context The context for the channel to be opened.
 * @param onReceive The function to be executed when an update is received.
 * @return The opened [ReceiveChannel].
 */
public fun <T> BroadcastChannel<T>.consumeAndReturn(
    scope: CoroutineScope,
    onReceive: suspend (T) -> Unit
): ReceiveChannel<T> {
    val subChannel = openSubscription()
    scope.launch {
        subChannel.consume { for (x in this) onReceive(x) }
    }
    return subChannel
}

public class MutexValue<V : Any>(@PublishedApi internal val value: V, @PublishedApi internal val mutex: Mutex) {

    public suspend inline fun <R> withLock(block: (value: V) -> R): R = mutex.withLock {
        block(value)
    }

}
