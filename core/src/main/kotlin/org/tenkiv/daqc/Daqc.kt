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

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.normalToOrNull
import org.tenkiv.daqc.hardware.definitions.device.Device


typealias Measurement = ValueInstant<Kuant>
typealias BinaryStateMeasurement = ValueInstant<BinaryState>
typealias QuantityMeasurement<Q> = ValueInstant<DaqcQuantity<Q>>

internal val daqcThreadContext = newSingleThreadContext("Main Daqc Context")

val daqcCriticalErrorBroadcastChannel = ConflatedBroadcastChannel<DaqcCriticalError>()

interface DaqcGate<out T : DaqcValue> : Updatable<ValueInstant<T>> {
    val isActive: Boolean

    fun deactivate()
}

interface IOStrand<out T : Kuant> : DaqcGate<T>

interface RangedIOStrand<T> : IOStrand<T> where T : Kuant, T : Comparable<T> {

    /**
     * The range of values that this IOStrand is likely to handle. There is not necessarily a guarantee that there will
     * never be a value outside this range.
     */
    val valueRange: ClosedRange<T>

    //TODO: Add more versions of this function, like one that suspends until it has a value.
    /**
     * @return a [Double] representation of the current value normalised to be between 0 and 1 based on the
     * [valueRange], null if the updatable does not yet have a value or the value is outside the [valueRange].
     */
    fun getNormalisedDoubleOrNull(): Double? {
        val value = valueOrNull?.value

        if (value is BinaryState?) return value?.toDouble()

        val min = valueRange.start.toDoubleInSystemUnit()
        val max = valueRange.endInclusive.toDoubleInSystemUnit()
        val valueDouble = value?.toDoubleInSystemUnit()

        return valueDouble?.normalToOrNull(min..max)
    }

}

open class DaqcException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)


sealed class DaqcCriticalError : DaqcException() {

    abstract val device: Device

    data class FailedToReinitialize(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    data class FailedMajorCommand(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    data class TerminalConnectionDisruption(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    data class PartialDisconnection(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

}
