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

import kotlinx.coroutines.flow.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.lib.physikal.*
import physikal.*
import java.time.*
import java.time.Duration
import kotlin.time.*

public fun DaqcChannel<*>.runningAverageUpdateRate(
    avgPeriod: Duration = 10.seconds.toJavaDuration()
): Flow<Quantity<Frequency>> {
    fun clean(sampleInstants: MutableList<Instant>) {
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

    return flow {
        val sampleInstants = ArrayList<Instant>()

        onEachUpdate {
            sampleInstants += it.instant
            clean(sampleInstants)

            val sps = sampleInstants.size / (avgPeriod.toMillis() * 1_000.0)
            emit(sps.hertz)
        }
    }

}