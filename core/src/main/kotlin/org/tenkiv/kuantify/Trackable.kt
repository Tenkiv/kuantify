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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import java.time.*
import javax.measure.quantity.*
import kotlin.coroutines.*
import kotlin.reflect.*

typealias TrackableQuantity<Q> = Trackable<ComparableQuantity<Q>>
typealias InitializedTrackableQuantity<Q> = InitializedTrackable<ComparableQuantity<Q>>

/**
 * The base interface which defines objects which have the ability to update their status.
 */
interface Trackable<out T> : CoroutineScope {

    /**
     * The [ConflatedBroadcastChannel] over which updates are broadcast.
     */
    val updateBroadcaster: ConflatedBroadcastChannel<out T>

}

fun <T> Trackable<ValueInstant<T>>.addTrigger(
    condition: (ValueInstant<T>) -> Boolean,
    onTrigger: () -> Unit
): Trigger<out T> =
    Trigger(
        triggerConditions = *arrayOf(TriggerCondition(this, condition)),
        triggerFunction = onTrigger
    )

interface InitializedTrackable<out T> : Trackable<T> {
    val value: T
}

/**
 * Gets the current value or returns Null.
 *
 * @return The value or null.
 */
val <T> Trackable<T>.valueOrNull get() = updateBroadcaster.valueOrNull

/**
 * Gets the current value or suspends and waits for one to exist.
 *
 * @return The current value.
 */
suspend fun <T> Trackable<T>.getValue(): T =
    updateBroadcaster.valueOrNull ?: updateBroadcaster.openSubscription().receive()

interface RatedTrackable<out T> : Trackable<ValueInstant<T>> {
    val updateRate: UpdateRate
}

sealed class UpdateRate(rate: TrackableQuantity<Frequency>) : TrackableQuantity<Frequency> by rate {

    class RunningAverage internal constructor(rate: TrackableQuantity<Frequency>) : UpdateRate(rate)

    /**
     * Means the update rate is a result of some set configuration and will only change when that configuration is
     * changed.
     */
    class Configured(rate: TrackableQuantity<Frequency>) : UpdateRate(rate)

}

fun RatedTrackable<*>.runningAverage(avgPeriod: Duration = 1.minutesSpan): AverageUpdateRateDelegate =
    AverageUpdateRateDelegate(this, avgPeriod)

class AverageUpdateRateDelegate internal constructor(input: RatedTrackable<*>, private val avgPeriod: Duration) {
    private val updatable: InitializedUpdatable<ComparableQuantity<Frequency>> = input.Updatable(0.hertz)

    init {
        input.launch {
            var updateRate: ComparableQuantity<Frequency>
            val sampleInstants = ArrayList<Instant>()

            input.updateBroadcaster.consumeEach {
                sampleInstants += it.instant
                clean(sampleInstants)

                val sps = sampleInstants.size / (avgPeriod.toMillis() * 1_000.0)
                updateRate = sps.hertz
                updatable.set(updateRate)
            }
        }
    }

    private fun clean(sampleInstants: MutableList<Instant>) {
        val iterator = sampleInstants.listIterator()
        while (iterator.hasNext()) {
            val instant = iterator.next()
            if (instant.isOlderThan(avgPeriod)) {
                iterator.remove()
            } else {
                break
            }
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): UpdateRate.RunningAverage =
        UpdateRate.RunningAverage(updatable)
}

private class CombinedTrackable<out T>(scope: CoroutineScope, trackables: Array<out Trackable<T>>) :
    Trackable<T> {

    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    private val _broadcastChannel = ConflatedBroadcastChannel<T>()

    override val updateBroadcaster: ConflatedBroadcastChannel<out T> get() = _broadcastChannel

    init {
        trackables.forEach { updatable ->
            launch {
                updatable.updateBroadcaster.consumeEach { update ->
                    _broadcastChannel.send(update)
                }
            }
        }
    }

    fun cancel() = job.cancel()
}

fun <T> CoroutineScope.CombinedTrackable(vararg trackables: Trackable<T>): Trackable<T> =
    CombinedTrackable(this, trackables)