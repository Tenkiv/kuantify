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
import org.tenkiv.kuantify.trackable.*

public abstract class ProcessedAcquireGate<T : DaqcData, ParentT : DaqcData>(
    initialMeasurement: ValueInstant<T>? = null
) : AcquireChannel<T> {
    protected abstract val parentGate: DaqcGate

    @Volatile
    private var _valueOrNull: ValueInstant<T>? = initialMeasurement
    public final override val valueOrNull: ValueInstant<T>?
        get() = _valueOrNull

    private val broadcastChannel = BroadcastChannel<ValueInstant<T>>(capacity = Channel.BUFFERED)
    protected open val failureBroadcastChannel: BroadcastChannel<FailedMeasurement>? = BroadcastChannel(capacity = 5)

    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = parentGate.isTransceiving

    public override val isFinalized: InitializedTrackable<Boolean>
        get() = parentGate.isFinalized

    public override fun openSubscription(): ReceiveChannel<ValueInstant<T>> = broadcastChannel.openSubscription()

    public override fun openProcessFailureSubscription(): ReceiveChannel<FailedMeasurement>? =
        failureBroadcastChannel?.openSubscription()

    public override suspend fun stopTransceiving() {
        parentGate.stopTransceiving()
    }

    public override fun finalize() {
        parentGate.finalize()
    }

    protected abstract suspend fun transformInput(input: ParentT): Result<T, ProcessFailure>

    protected abstract fun openParentSubscription(): ReceiveChannel<ValueInstant<ParentT>>

    protected open suspend fun transformationFailure(failure: FailedMeasurement) {
        failureBroadcastChannel?.send(failure)
    }

    /**
     * This isn't called in init because it is possible that parentGate could be uninitialized at the time it's used
     * in this function. It should be called in the constructor of the first class in the chain that has a
     * final parentGate.
     */
    protected fun initCoroutines() {
        launch {
            openParentSubscription().consumingOnEach { measurement ->
                when (val convertedResult = transformInput(measurement.value)) {
                    is Result.Success -> update(convertedResult.value at measurement.instant)
                    is Result.Failure -> transformationFailure(convertedResult.error at measurement.instant)
                }
            }
        }
        if (parentGate is AcquireChannel<*>) {
            parentGate.processFailureHandler {
                failureBroadcastChannel?.send(it) ?: throw IllegalStateException(
                    "If parent gate can have a process failures, child process gate must propagate those failures."
                )
            }
        }
    }

    private suspend fun update(updated: ValueInstant<T>) {
        _valueOrNull = updated
        broadcastChannel.send(updated)
    }
}

public abstract class ProcessedAcquireChannel<T : DaqcData, ParentT : DaqcData>(
    initialMeasurement: ValueInstant<T>? = null
) : ProcessedAcquireGate<T, ParentT>(initialMeasurement) {
    protected abstract override val parentGate: AcquireChannel<ParentT>

    protected final override fun openParentSubscription(): ReceiveChannel<ValueInstant<ParentT>> =
        parentGate.openSubscription()

    public final override fun startSampling() {
        parentGate.startSampling()
    }

}