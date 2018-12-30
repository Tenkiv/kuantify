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

package org.tenkiv.kuantify

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.isOlderThan
import org.tenkiv.coral.minutesSpan
import org.tenkiv.physikal.core.*
import tec.units.indriya.ComparableQuantity
import java.time.Duration
import java.time.Instant
import javax.measure.quantity.Frequency
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KProperty

/**
 * The base interface which defines objects which have the ability to update their status.
 */
interface Updatable<out T> : CoroutineScope {

    /**
     * The [ConflatedBroadcastChannel] over which updates are broadcast.
     */
    val broadcastChannel: ConflatedBroadcastChannel<out T>

}

/**
 * Gets the current value of the [broadcastChannel] or returns Null.
 *
 * @return The value of the [broadcastChannel] or null.
 */
val <T> Updatable<T>.valueOrNull get() = broadcastChannel.valueOrNull

/**
 * Gets the current value of the [broadcastChannel] or suspends and waits for one to exist.
 *
 * @return The value of the [broadcastChannel]
 */
suspend fun <T> Updatable<T>.getValue(): T =
    broadcastChannel.valueOrNull ?: broadcastChannel.openSubscription().receive()

interface RatedUpdatable<out T> : Updatable<ValueInstant<T>> {
    val updateRate: ComparableQuantity<Frequency>
}

fun RatedUpdatable<*>.runningAverage(avgLength: Duration = 1.minutesSpan): AverageUpdateRateDelegate =
    AverageUpdateRateDelegate(this, avgLength)

class AverageUpdateRateDelegate internal constructor(input: RatedUpdatable<*>, private val avgLength: Duration) {

    @Volatile
    var updateRate = 0.hertz

    init {
        input.launch {
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

private class CombinedUpdatable<out T>(scope: CoroutineScope, updatables: Array<out Updatable<T>>) :
    Updatable<T> {

    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    private val _broadcastChannel = ConflatedBroadcastChannel<T>()

    override val broadcastChannel: ConflatedBroadcastChannel<out T> get() = _broadcastChannel

    init {
        updatables.forEach { updatable ->
            launch {
                updatable.broadcastChannel.consumeEach { update ->
                    _broadcastChannel.send(update)
                }
            }
        }
    }

    fun cancel() = job.cancel()
}

fun <T> CoroutineScope.CombinedUpdatable(vararg updatables: Updatable<T>): Updatable<T> =
    CombinedUpdatable(this, updatables)