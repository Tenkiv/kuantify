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
import org.tenkiv.coral.normalTo
import org.tenkiv.daqc.hardware.definitions.device.Device


typealias Measurement = ValueInstant<DaqcValue>
typealias BinaryStateMeasurement = ValueInstant<BinaryState>
typealias QuantityMeasurement<Q> = ValueInstant<DaqcQuantity<Q>>

/**
 * The default context which the library uses to handle data.
 */
internal val daqcThreadContext = newSingleThreadContext("Main Daqc Context")

/**
 * The [ConflatedBroadcastChannel] which sends [DaqcCriticalError] messages notifying of potentially critical issues.
 * This channel should be monitored closely.
 */
val daqcCriticalErrorBroadcastChannel = ConflatedBroadcastChannel<DaqcCriticalError>()

/**
 * An [Updatable] which returns values in the form of [ValueInstant]s.
 */
interface IOChannel<out T : DaqcValue> : Updatable<ValueInstant<T>> {

    /**
     * If the channel is activated or not.
     */
    val isActive: Boolean

    /**
     * Function to deactivate the channel.
     */
    fun deactivate()
}

/**
 * A [RangedIOChannel] whose type implements both [DaqcValue] and [Comparable].
 */
interface RangedIOChannel<T> : IOChannel<T> where T : DaqcValue, T : Comparable<T> {

    /**
     * The range of values that this IO is likely to handle. There is not necessarily a guarantee that there will
     * never be a value outside this range.
     */
    val valueRange: ClosedRange<T>

    /**
     * @return a [Double] representation of the current value normalised to be between 0 and 1 based on the
     * [valueRange], null if the updatable does not yet have a value or the value is outside the [valueRange].
     */
    fun getNormalisedDoubleOrNull(): Double? {
        val value = valueOrNull?.value

        if (value is BinaryState?)
            return value?.toDouble()

        val min = valueRange.start.toDoubleInSystemUnit()
        val max = valueRange.endInclusive.toDoubleInSystemUnit()
        val valueDouble = value?.toDoubleInSystemUnit()

        return if (valueDouble != null && valueDouble >= min && valueDouble <= max) {
            valueDouble normalTo min..max
        } else {
            null
        }
    }

}

/**
 * Base class for Exceptions to be thrown on Channels handling [DaqcValue]s.
 */
open class DaqcException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

/**
 * Class handling a set of errors which are extremely serious and represent major issues to be handled.
 */
sealed class DaqcCriticalError : DaqcException() {

    /**
     * The [Device] which has thrown the error.
     */
    abstract val device: Device

    /**
     * Error where a board which was told to restart, reboot, or restore was not recovered or failed in the reboot
     * process.
     */
    data class FailedToReinitialize(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    /**
     * Error where a command issued to the board was not able to be executed.
     */
    data class FailedMajorCommand(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    /**
     * Error representing a fatal termination of the connection to the [Device].
     */
    data class TerminalConnectionDisruption(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    /**
     * Error representing a serious lapse in the connection to a board, but not terminal.
     */
    data class PartialDisconnection(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()
}
