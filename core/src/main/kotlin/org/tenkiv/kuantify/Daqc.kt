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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.newSingleThreadContext
import org.tenkiv.coral.ValueInstant
import org.tenkiv.kuantify.data.BinaryState
import org.tenkiv.kuantify.data.DaqcQuantity
import org.tenkiv.kuantify.data.DaqcValue
import org.tenkiv.kuantify.hardware.definitions.device.Device


typealias Measurement = ValueInstant<DaqcValue>
typealias BinaryStateMeasurement = ValueInstant<BinaryState>
typealias QuantityMeasurement<Q> = ValueInstant<DaqcQuantity<Q>>

private val daqcDispatcherThread: CoroutineDispatcher = newSingleThreadContext("DaqcSingleThreadDispatcher")

/**
 * The default dispatcher which kuantify uses to handle data.
 * This coroutine dispatcher uses only a single thread,
 * thus thread blocking calls and long running computations must never
 * be executed on it. It should only be used for suspending IO bound operations where the default multithreaded context
 * is not a viable option, such as when multiple coroutines share some mutable state that is not thread safe. It never
 * has to be used, another single thread dispatcher or a UI dispatcher could be used just as well if they are available.
 *
 * @see <a href="https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#thread-confinement-coarse-
 * grained">Thread confinement coarse-grained</a>
 */
val Dispatchers.Daqc: CoroutineDispatcher get() = daqcDispatcherThread

/**
 * The [ConflatedBroadcastChannel] which sends [DaqcCriticalError] messages notifying of potentially critical issues.
 * This channel should be monitored closely.
 */
val daqcCriticalErrorBroadcastChannel = ConflatedBroadcastChannel<DaqcCriticalError>()

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
