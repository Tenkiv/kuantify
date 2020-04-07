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

package org.tenkiv.kuantify.gate.acquire

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.*

public typealias FailedMeasurement = ValueInstant<ProcessFailure>
public typealias SuccesfulProcessResult<T> = Result.Success<ValueInstant<T>>
public typealias ProcessResult<ST> = Result<ValueInstant<ST>, FailedMeasurement>

public interface AcquireGate<out T : DaqcData> : DaqcGate<T>, UpdateRatedGate<T> {
    /**
     * Activates the input alerting it to begin collecting and sending data.
     */
    public fun startSampling()

    /**
     * Opens a subscription to a broadcast that reports failures in processing the underlying data for this
     * [AcquireGate].
     *
     * @return The subscription to the broadcast or null if this [AcquireGate] does no processing that can fail.
     */
    public fun openProcessFailureSubscription(): ReceiveChannel<FailedMeasurement>?

}

public inline fun AcquireGate<*>.processFailureHandler(
    scope: CoroutineScope = this,
    crossinline onFailure: suspend (FailedMeasurement) -> Unit
) {
    val channel = openProcessFailureSubscription()
    if (channel != null) {
        scope.launch {
            channel.consumingOnEach { onFailure(it) }
        }
    }
}