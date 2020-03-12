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
import kotlinx.coroutines.flow.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import java.util.concurrent.atomic.*
import kotlin.properties.*
import kotlin.reflect.*

public typealias SuccesfulMeasurement<T> = ValueInstant<T>
public typealias FailedMeasurement = ValueInstant<ProcessFailure>
public typealias ProcessResult<ST> = Result<SuccesfulMeasurement<ST>, FailedMeasurement>

public interface AcquireGate<out T : DaqcData> : DaqcGate<T>, RatedTrackable<T> {

    /**
     * Broadcasts failures to process updates from an underlying data source resulting in an inability to produce an
     * updated value for this gate.
     * This will not indicate critical errors in the underlying DAQC system, for that see
     * [criticalDaqcErrorBroadcaster].
     * This is a hot [Flow] backed by a [kotlinx.coroutines.channels.BroadcastChannel] and
     * as such can be consumed an unlimited number of times and continue to provide updates to all consumers.
     *
     * If it is null it means this gate either does no processing to the underlying data the update is based on or the
     * processing cannot fail.
     */
    public val processFailureBroadcaster: Flow<FailedMeasurement>? get() = null

    /**
     * Broadcasts the result of processing an underlying data source for this gate. The latest [Result.Success] value
     * will be identical to the latest [updateBroadcaster] value and [Result.Failure] value will be identical to the
     * latest [processFailureBroadcaster] value.
     * This will not indicate critical errors in the underlying DAQC system, for that see
     * [criticalDaqcErrorBroadcaster].
     * This is a hot [Flow] backed by a [kotlinx.coroutines.channels.BroadcastChannel] and
     * as such can be consumed an unlimited number of times and continue to provide updates to all consumers.
     *
     * If [processFailureBroadcaster] is null this will be identical to [updateBroadcaster] but with all the updates
     * wrapped in [Result.Success].
     */
    public val processResultBroadcaster: Flow<ProcessResult<T>>
        get() = updateBroadcaster.asFlow().map { Result.Success(it) }

    /**
     * Activates the input alerting it to begin collecting and sending data.
     */
    public fun startSampling()

}

/**
 * Creates a delegate that will broadcast updates as [Result.Success] and process failures as [Result.Failure].
 * i.e. it combines [AcquireGate.updateBroadcaster] and [AcquireGate.processFailureBroadcaster] into a single [Flow]
 * representing the [Result] of each update for you.
 */
public fun <T : DaqcData> AcquireGate<T>.combineBroadcasters(): CombinedBroadcaster<T> = CombinedBroadcaster()

public interface ProcessFailure {
    public val cause: Throwable
}

//TODO: There should be a better way to do this with some kind of Flow combining operation.
public class CombinedBroadcaster<ST : DaqcData> : ReadOnlyProperty<AcquireGate<ST>, Flow<ProcessResult<ST>>> {
    private var initialized = AtomicBoolean(false)
    private val broadcaster = BroadcastChannel<ProcessResult<ST>>(Channel.Factory.BUFFERED)

    public override fun getValue(
        thisRef: AcquireGate<ST>,
        property: KProperty<*>
    ): Flow<ProcessResult<ST>> {
        if (!initialized.get()) init(thisRef)
        return broadcaster.asFlow()
    }

    private fun init(gate: AcquireGate<ST>) {
        initialized.set(true)
        gate.launch {
            gate.updateBroadcaster.consumeEach { success ->
                broadcaster.send(Result.Success(success))
            }
        }
        gate.launch {
            gate.processFailureBroadcaster?.collect { failure ->
                broadcaster.send(Result.Failure(failure))
            }
        }
    }

}