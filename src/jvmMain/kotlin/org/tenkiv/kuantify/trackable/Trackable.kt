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

package org.tenkiv.kuantify.trackable

import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.lib.*
import physikal.*

public typealias TrackableQuantity<QT> = Trackable<Quantity<QT>>
public typealias InitializedTrackableQuantity<Q> = InitializedTrackable<Quantity<Q>>

/**
 * The base interface which defines objects which have the ability to update their value.
 */
public interface Trackable<out T : Any> {
    /**
     * Gets the current value or returns Null.
     *
     * @return The value or null.
     */
    public val valueOrNull: T?

    /**
     * Creates a subscription to updates to this [Trackable].
     */
    public fun openSubscription(): ReceiveChannel<T>
}

public suspend inline fun <T : Any> Trackable<T>.onEachUpdate(action: (update: T) -> Unit) =
    openSubscription().consumingOnEach(action)

public interface InitializedTrackable<out T : Any> : Trackable<T> {
    val value: T
}

/**
 * Gets the current value or suspends and waits for one to exist.
 *
 * @return The current value.
 */
public suspend fun <T : Any> Trackable<T>.getValue(): T = valueOrNull ?: openSubscription().consume { receive() }