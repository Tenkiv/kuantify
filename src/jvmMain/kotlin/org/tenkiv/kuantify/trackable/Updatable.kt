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
import physikal.*

public typealias UpdatableQuantity<QT> = Updatable<Quantity<QT>>
public typealias UpdatableSetter<T> = Updatable.ValueSetter<T>.(value: T) -> Unit

/**
 * Same as [Trackable] but allows setting.
 * Buffer is always [Channel.CONFLATED].
 */
public interface Updatable<T : Any> : Trackable<T> {

    public fun set(value: T)

    //TODO: Make fun interface in kotlin 1.4
    public interface ValueSetter<T> {

        public fun setValue(value: T)

    }
}

private class UpdatableImpl<T : Any> : Updatable<T> {
    private val broadcastChannel = ConflatedBroadcastChannel<T>()
    override val valueOrNull: T?
        get() = broadcastChannel.valueOrNull

    override fun openSubscription(): ReceiveChannel<T> = broadcastChannel.openSubscription()

    override fun set(value: T) {
        broadcastChannel.offer(value)
    }
}

private class CustomSetUpdatable<T : Any>(private val customSetter: UpdatableSetter<T>) : Updatable<T> {
    private val broadcastChannel = ConflatedBroadcastChannel<T>()
    override val valueOrNull: T?
        get() = broadcastChannel.valueOrNull

    private val setValue = object : Updatable.ValueSetter<T> {
        override fun setValue(value: T) {
            broadcastChannel.offer(value)
        }
    }

    override fun openSubscription(): ReceiveChannel<T> = broadcastChannel.openSubscription()

    override fun set(value: T) {
        setValue.customSetter(value)
    }
}

public fun <T : Any> Updatable(): Updatable<T> = UpdatableImpl()

public fun <T : Any> Updatable(setter: UpdatableSetter<T>): Updatable<T> = CustomSetUpdatable(setter)
