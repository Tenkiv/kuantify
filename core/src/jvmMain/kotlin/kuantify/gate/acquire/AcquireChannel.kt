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

package kuantify.gate.acquire

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kuantify.data.*
import kuantify.gate.*
import kuantify.lib.*
import org.tenkiv.coral.*

public typealias FailedMeasurement = ValueInstant<ProcessFailure>
public typealias ProcessResult<ST> = Result<Measurement<ST>, FailedMeasurement>

public interface AcquireChannel<out T : DaqcData> : DaqcChannel<T> {
    /**
     * Activates the input alerting it to begin collecting and sending data.
     */
    public fun startSampling()

    /**
     * Flow of failures in processing the underlying data for this [AcquireChannel].
     * Will be null if this [AcquireChannel] does no processing that can fail.
     */
    public val processFailureFlow: SharedFlow<FailedMeasurement>?

}

/**
 * @return null if the associated [AcquireChannel] does no processing that can fail.
 */
public inline fun AcquireChannel<*>.processFailureHandler(
    scope: CoroutineScope = this,
    crossinline onFailure: suspend (failure: FailedMeasurement) -> Unit
): Unit? {
    val flow = processFailureFlow
    return if (flow != null) {
        scope.launch {
            flow.collect { onFailure(it) }
        }
        Unit
    } else {
        null
    }
}
