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

import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import tec.units.indriya.ComparableQuantity
import java.time.Instant
import javax.measure.quantity.Frequency

/**
 * Interface for anything that can be accurately represented as a [DaqcValue] or multiple [DaqcValue]s.
 *
 * All implementations need to be **completely immutable** to function properly.
 */
interface DaqcData {
    /**
     * The number of [DaqcValue]s this DaqcData is represented by.
     */
    val size: Int

    // TODO: Keep looking / waiting for similar data structure that forces immutability
    fun toDaqcValueList(): List<DaqcValue>

    // TODO: Keep looking / waiting for similar data structure that forces immutability
    fun toDaqcValueMap(): Map<String, DaqcValue>
}

sealed class LineNoiseFrequency {

    data class AccountFor(val frequency: ComparableQuantity<Frequency>) : LineNoiseFrequency()

    object Ignore : LineNoiseFrequency()

}

enum class DigitalStatus {
    ACTIVATED_STATE,
    ACTIVATED_FREQUENCY,
    ACTIVATED_PWM,
    DEACTIVATED
}

data class PrimitiveValueInstant(val epochMilli: Long, val value: String) {

    inline fun <T> toValueInstant(deserializeValue: (String) -> T): ValueInstant<T> =
        deserializeValue(value) at Instant.ofEpochMilli(epochMilli)

}
