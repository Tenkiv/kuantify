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
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.trackable.*


public interface DaqcGate : Finalizable, CoroutineScope {
    public val isTransceiving: Trackable<Boolean>

    public fun stopTransceiving()

}

public interface DaqcChannel<out T : DaqcData> : DaqcGate {
    /**
     * Number of [DaqcValue]s in the [DaqcData] handled by this [DaqcChannel]
     */
    public val daqcDataSize: Int32

    /**
     * Gets the current value or returns null.
     *
     * @return The value or null.
     */
    public val valueOrNull: ValueInstant<T>?

    /**
     * Creates a subscription to updates to this [DaqcChannel]. This [Channel] will receive all future updates but will
     * not immediately receive the current value of this [DaqcChannel]
     */
    public fun openSubscription(): ReceiveChannel<ValueInstant<T>>
}

public suspend inline fun <T : DaqcData> DaqcChannel<T>.onEachUpdate(action: (update: ValueInstant<T>) -> Unit): Unit =
    openSubscription().consumingOnEach(action)

/**
 * Gets the current value or suspends and waits for one to exist.
 *
 * @return The current value.
 */
public suspend fun <T : DaqcData> DaqcChannel<T>.getValue(): ValueInstant<T> =
    valueOrNull ?: openSubscription().consume { receive() }

/**
 * A [DaqcChannel] which only has a single data parameter.
 */
public interface IOStrand<out T : DaqcValue> : DaqcChannel<T> {
    /**
     * Number of [DaqcValue]s in the [DaqcData] handled by this [DaqcChannel]
     *
     * [IOStrand]s only work with [DaqcValue] so this is always 1 in all [IOStrand]s
     */
    public override val daqcDataSize: Int32 get() = 1
}

public interface RangedIOStrand<T> : IOStrand<T> where T : DaqcValue, T : Comparable<T> {
    /**
     * The range of values that this IOStrand is expected to handle. There is not an enforced guarantee that there will
     * never be a value outside this range.
     */
    public val valueRange: ClosedRange<T>
}

//TODO: Add more versions of this function, like one that suspends until it has a value.
/**
 * @return a [Float64] representation of the current value normalised to be between 0 and 1 based on the
 * [valueRange], null if the updatable does not yet have a value or the value is outside the [valueRange].
 */
public fun RangedIOStrand<*>.getNormalisedFloat64OrNull(): Float64? {
    val value = valueOrNull?.value

    if (value is BinaryState?) return value?.toFloat64()

    val min = valueRange.start.toFloat64InDefaultUnit()
    val max = valueRange.endInclusive.toFloat64InDefaultUnit()
    val valueFloat64 = value?.toFloat64InDefaultUnit()

    return valueFloat64?.normalToOrNull(min..max)
}