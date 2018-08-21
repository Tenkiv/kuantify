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

package org.tenkiv.daqc

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.isOlderThan
import org.tenkiv.coral.minutesSpan
import org.tenkiv.physikal.core.hertz
import tec.units.indriya.ComparableQuantity
import java.time.Duration
import java.time.Instant
import javax.measure.quantity.Frequency
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KProperty

/**
 * The base interface which defines objects which have the ability to update their status.
 */
interface Updatable<out T> {

    /**
     * The [ConflatedBroadcastChannel] over which updates are broadcast.
     */
    val broadcastChannel: ConflatedBroadcastChannel<out T>

    /**
     * Gets the current value of the [broadcastChannel] or returns Null.
     *
     * @return The value of the [broadcastChannel] or null.
     */
    val valueOrNull get() = broadcastChannel.valueOrNull

    /**
     * Gets the current value of the [broadcastChannel] or suspends and waits for one to exist.
     *
     * @return The value of the [broadcastChannel]
     */
    suspend fun getValue(): T = broadcastChannel.valueOrNull ?: broadcastChannel.openSubscription().receive()

    /**
     * Function to open a channel to consume updates of the [broadcastChannel].
     *
     * @param context The [CoroutineContext] upon which to consume the updates in.
     * @param onUpdate The function to execute when an update is received.
     * @return The [Job] which represents the coroutine consuming the data.
     */
    fun openNewCoroutineListener(context: CoroutineContext = DefaultDispatcher, onUpdate: suspend (T) -> Unit): Job =
        launch(context) { broadcastChannel.consumeEach { onUpdate(it) } }
}

interface RatedUpdatable<out T> : Updatable<ValueInstant<T>> {
    val updateRate: ComparableQuantity<Frequency>
}

fun RatedUpdatable<*>.runningAverage(avgLength: Duration = 1.minutesSpan): AverageSampleRateDelegate =
    AverageSampleRateDelegate(this, avgLength)

class AverageSampleRateDelegate internal constructor(input: RatedUpdatable<*>, private val avgLength: Duration) {

    @Volatile
    var updateRate = 0.hertz

    init {
        launch {
            val sampleInstants = ArrayList<Instant>()

            input.broadcastChannel.consumeEach {
                sampleInstants += it.instant
                clean(sampleInstants)

                val sps = sampleInstants.size / (avgLength.toMillis() * 1_000.0)
                updateRate = sps.hertz
            }
        }
    }

    private fun clean(sampleInstants: MutableList<Instant>) {
        val iterator = sampleInstants.listIterator()
        while (iterator.hasNext()) {
            val instant = iterator.next()
            if (instant.isOlderThan(avgLength)) {
                iterator.remove()
            } else {
                break
            }
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): ComparableQuantity<Frequency> = updateRate
}