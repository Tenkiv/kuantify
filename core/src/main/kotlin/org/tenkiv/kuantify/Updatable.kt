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

package org.tenkiv.kuantify

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import tec.units.indriya.*
import kotlin.coroutines.*

typealias UpdatableQuantity<Q> = Updatable<ComparableQuantity<Q>>
typealias InitializedUpdatableQuantity<Q> = InitializedUpdatable<ComparableQuantity<Q>>

/**
 * Same as [Trackable] but allows setting.
 */
interface Updatable<T> : Trackable<T> {

    override val updateBroadcaster: ConflatedBroadcastChannel<out T>

    fun set(value: T)

}

interface InitializedUpdatable<T> : Updatable<T>, InitializedTrackable<T> {

    override var value: T

}

private class SimpleUpdatable<T>(scope: CoroutineScope) : Updatable<T> {

    override val coroutineContext: CoroutineContext = scope.coroutineContext

    private val _updateBroadcaster = ConflatedBroadcastChannel<T>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out T> get() = _updateBroadcaster

    override fun set(value: T) {
        _updateBroadcaster.offer(value)
    }
}

private class SimpleInitializedUpdatable<T>(scope: CoroutineScope, initialValue: T) : InitializedUpdatable<T> {

    override val coroutineContext: CoroutineContext = scope.coroutineContext

    private val _updateBroadcaster = ConflatedBroadcastChannel(initialValue)
    override val updateBroadcaster: ConflatedBroadcastChannel<out T> get() = _updateBroadcaster

    override var value: T
        get() = updateBroadcaster.value
        set(value) = set(value)

    override fun set(value: T) {
        _updateBroadcaster.offer(value)
    }
}

fun <T> CoroutineScope.Updatable(): Updatable<T> = SimpleUpdatable(this)

fun <T> CoroutineScope.Updatable(initialValue: T): InitializedUpdatable<T> =
    SimpleInitializedUpdatable(this, initialValue)