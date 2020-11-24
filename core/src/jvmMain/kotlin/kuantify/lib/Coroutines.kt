/*
 * Copyright 2020 Tenkiv, Inc.
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

package kuantify.lib

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*

/**
 * [consume] channel and execute [action] on each element. While chains of operators should be implemented using [Flow]
 * many situation can be handled by simply taking an action on every element of a channel and this function provides
 * a convenient, safe way to do that.
 */
public suspend inline fun <E> ReceiveChannel<E>.consumingOnEach(action: (element: E) -> Unit): Unit = consume {
    for (element in this) {
        action(element)
    }
}

/**
 * Creates a new [CoroutineScope] which is identical to the current one but the [Job] of its context replaced with a
 * new [CompletableJob] that is a child of the current one.
 *
 * Generally used to make a scope that can be canceled independently of the parent scope.
 */
public fun CoroutineScope.withNewChildJob(): CoroutineScope = this + Job(this.coroutineContext[Job])

public class MutexValue<V : Any>(@PublishedApi internal val value: V, @PublishedApi internal val mutex: Mutex) {

    public suspend inline fun <R> withLock(block: (value: V) -> R): R = mutex.withLock {
        block(value)
    }

}

/**
 * Combines two flows of any type by transforming all values from either flow to the same type and putting them into a
 * new [Flow].
 */
public inline fun <A, B, R> transformEitherIn(
    scope: CoroutineScope,
    a: Flow<A>,
    b: Flow<B>,
    crossinline transformA: (A) -> R,
    crossinline transformB: (B) -> R
): Flow<R> = channelFlow {
    scope.launch {
        a.collect {
            send(transformA(it))
        }
    }
    scope.launch {
        b.collect {
            send(transformB(it))
        }
    }
}
