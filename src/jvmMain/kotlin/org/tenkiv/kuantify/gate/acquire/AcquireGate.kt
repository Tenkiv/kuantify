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

import kotlinx.coroutines.flow.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import kotlin.properties.*
import kotlin.reflect.*

public typealias FailedMeasurement = ValueInstant<ProcessFailure>
public typealias SuccesfulProcessResult<T> = Result.Success<ValueInstant<T>>
public typealias ProcessResult<ST> = Result<ValueInstant<ST>, FailedMeasurement>

public interface AcquireGate<out T : DaqcData> : DaqcGate<T>, RatedTrackable<T> {

    /**
     * Broadcasts the result of processing an underlying data source for this gate. The latest [Result.Success] value
     * will be identical to the latest [updateBroadcaster] value and [Result.Failure] value will be identical to the
     * latest [processFailureBroadcaster] value.
     * This will not indicate critical errors in the underlying DAQC system, for that see
     * [criticalDaqcErrorBroadcaster].
     * This is a hot [Flow] backed by a [kotlinx.coroutines.channels.BroadcastChannel], each call to [Flow.collect]
     * means there will be a new subscription made.
     *
     * If [processFailureBroadcaster] is null this will be identical to [updateBroadcaster] but with all the updates
     * wrapped in [Result.Success].
     */
    public val processResultBroadcaster: Flow<ProcessResult<T>>

    /**
     * Broadcasts failures to process updates from an underlying data source resulting in an inability to produce an
     * updated value for this gate.
     * This will not indicate critical errors in the underlying DAQC system, for that see
     * [criticalDaqcErrorBroadcaster].
     * This is a hot [Flow] backed by a [kotlinx.coroutines.channels.BroadcastChannel], each call to [Flow.collect]
     * means there will be a new subscription made.
     *
     * If this is null it means this gate either does no processing to the underlying data the update is based on or the
     * processing cannot fail.
     */
    public val processFailureBroadcaster: Flow<FailedMeasurement>? get() = null

    /**
     * Activates the input alerting it to begin collecting and sending data.
     */
    public fun startSampling()

}

public fun <T : DaqcData> AcquireGate<T>.succesfulResultBroadcaster(): SuccesfulResultBroadcaster<T> =
    SuccesfulResultBroadcaster(this)

public interface ProcessFailure {
    public val cause: Throwable
}

public class SuccesfulResultBroadcaster<ST : DaqcData>(gate: AcquireGate<ST>) :
    ReadOnlyProperty<AcquireGate<ST>, Flow<SuccesfulProcessResult<ST>>> {
    private val broadcastFlow = gate.updateBroadcaster.asFlow().map { Result.Success(it) }

    public override fun getValue(thisRef: AcquireGate<ST>, property: KProperty<*>): Flow<SuccesfulProcessResult<ST>> =
        broadcastFlow

}