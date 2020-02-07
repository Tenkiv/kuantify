/*
 * Copyright 2019 Tenkiv, Inc.
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
import mu.*
import org.tenkiv.kuantify.hardware.device.*


private val logger = KotlinLogging.logger {}

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
public val Dispatchers.Daqc: CoroutineDispatcher get() = daqcDispatcherThread

/**
 * The [ConflatedBroadcastChannel] which sends [CriticalDaqcError] messages notifying of potentially critical issues.
 * This channel should be monitored closely.
 */
public val criticalDaqcErrorBroadcaster: ConflatedBroadcastChannel<CriticalDaqcError> =
    ConflatedBroadcastChannel<CriticalDaqcError>()

/**
 * Class handling a set of errors which are extremely serious and represent major issues to be handled.
 */
public sealed class CriticalDaqcError {

    /**
     * The [Device] which has thrown the error.
     */
    public abstract val device: Device

    public abstract val message: String?

    /**
     * Error where a command issued to the board was not able to be executed.
     */
    public data class FailedMajorCommand(
        public override val device: Device,
        public override val message: String? = null
    ) : CriticalDaqcError()

    /**
     * Error representing a serious lapse in the connection to a board, but not terminal.
     */
    public data class PartialDisconnection(
        public override val device: Device,
        public override val message: String? = null,
        public val cause: Throwable? = null
    ) : CriticalDaqcError()

    /**
     * Error representing a fatal termination of the connection to the [Device].
     */
    public data class TerminalConnectionDisruption(
        public override val device: Device,
        public override val message: String? = null,
        public val cause: Throwable? = null
    ) : CriticalDaqcError()

    /**
     * Error where a board which was told to restart, reboot, or restore was not recovered or failed in the reboot
     * process.
     */
    public data class FailedToReinitialize(
        public override val device: Device,
        public override val message: String? = null,
        public val cause: Throwable? = null
    ) : CriticalDaqcError()
}
