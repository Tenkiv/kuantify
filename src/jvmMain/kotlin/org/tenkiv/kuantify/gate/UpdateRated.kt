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

package org.tenkiv.kuantify.gate

import kotlinx.coroutines.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.trackable.*
import physikal.*
import java.time.*
import kotlin.properties.*
import kotlin.reflect.*

public interface UpdateRatedGate<out T : DaqcData> : DaqcChannel<T> {
    public val updateRate: UpdateRate
}

public sealed class UpdateRate(rate: TrackableQuantity<Frequency>) : TrackableQuantity<Frequency> by rate {

    public class RunningAverage internal constructor(rate: TrackableQuantity<Frequency>) : UpdateRate(rate)

    /**
     * Means the update rate is a result of some set configuration and will only change when that configuration is
     * changed.
     */
    public class Configured(rate: TrackableQuantity<Frequency>) : UpdateRate(rate)

}

public fun UpdateRatedGate<*>.runningAverage(avgPeriod: Duration = 1.minutesSpan): AverageUpdateRateDelegate =
    AverageUpdateRateDelegate(this, avgPeriod)

public class AverageUpdateRateDelegate internal constructor(
    gate: UpdateRatedGate<*>,
    private val avgPeriod: Duration
) : ReadOnlyProperty<UpdateRatedGate<*>, UpdateRate.RunningAverage> {
    private val updatable = gate.Updatable(0.0.hertz)

    init {
        gate.launch {
            var updateRate: Quantity<Frequency>
            val sampleInstants = ArrayList<Instant>()

            //TODO: This can give a null pointer exception if UpdateRate is initialized before updateBroadcaster.
            gate.onEachUpdate {
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

    public override fun getValue(thisRef: UpdateRatedGate<*>, property: KProperty<*>): UpdateRate.RunningAverage =
        UpdateRate.RunningAverage(updatable)
}