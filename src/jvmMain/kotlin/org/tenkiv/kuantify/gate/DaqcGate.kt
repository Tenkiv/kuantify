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
import org.tenkiv.kuantify.trackable.*


public interface DaqcGate<out T : DaqcData> : Trackable<ValueInstant<T>>, CoroutineScope {
    /**
     * Number of [DaqcValue]s in the [DaqcData] handled by this [DaqcGate]
     */
    public val daqcDataSize: Int
    public val isTransceiving: InitializedTrackable<Boolean>
    public val isFinalized: InitializedTrackable<Boolean>

    public fun stopTransceiving()

    /**
     * Finalize the configuration of this [DaqcGate] so nothing can be changed for the remainder of its existence.
     * This will also finalize any [DaqcGate]s this [DaqcGate]s data is derived from.
     *
     * Attempts to modify the configuration of a [DaqcGate] after [finalize] will fail.
     *
     * This is an idempotent operation - subsequent calls to this function have no effect.
     */
    public fun finalize()
}

/**
 * Convenience function to wrap [DaqcGate] configuration modification calls in for automatic handling of
 * [DaqcGate.isFinalized]. This function should only be use when creating a new type of [DaqcGate].
 */
public inline fun <R> DaqcGate<*>.modifyConfiguration(block: () -> R): R = if (!isFinalized.value) {
    block()
} else {
    throw IllegalStateException("Cannot modify configuration of DaqcGate that has been finalized.")
}

public fun Iterable<DaqcGate<*>>.finalizeAll() {
    forEach { it.finalize() }
}

public fun Array<out DaqcGate<*>>.finalizeAll() {
    forEach { it.finalize() }
}

/**
 * A [DaqcGate] which only has a single data parameter.
 */
public interface IOStrand<out T : DaqcValue> : DaqcGate<T> {
    /**
     * Number of [DaqcValue]s in the [DaqcData] handled by this [DaqcGate]
     *
     * [IOStrand]s only work with [DaqcValue] so this is always 1 in all [IOStrand]s
     */
    public override val daqcDataSize get() = 1
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
 * @return a [Double] representation of the current value normalised to be between 0 and 1 based on the
 * [valueRange], null if the updatable does not yet have a value or the value is outside the [valueRange].
 */
public fun RangedIOStrand<*>.getNormalisedDoubleOrNull(): Double? {
    val value = valueOrNull?.value

    if (value is BinaryState?) return value?.toDouble()

    val min = valueRange.start.toDoubleInDefaultUnit()
    val max = valueRange.endInclusive.toDoubleInDefaultUnit()
    val valueDouble = value?.toDoubleInDefaultUnit()

    return valueDouble?.normalToOrNull(min..max)
}