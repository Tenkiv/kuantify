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
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import org.tenkiv.coral.*
import kuantify.data.*
import kuantify.gate.*
import kuantify.lib.*
import kuantify.trackable.*

public abstract class ProcessedAcquireGate<T : DaqcData, ParentT : DaqcData>(
    valueBufferCapacity: UInt32 = DEFAULT_VALUE_BUFFER_CAPACITY,
    failureBufferCapacity: UInt32 = DEFAULT_FAILURE_BUFFER_CAPACITY
) : AcquireChannel<T> {
    protected abstract val parentGate: DaqcGate
    protected abstract val parentValueFlow: Flow<ValueInstant<ParentT>>

    private val _valueFlow: MutableSharedFlow<ValueInstant<T>> =
        MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = valueBufferCapacity.toInt32(),
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    override val valueFlow: SharedFlow<ValueInstant<T>> get() = _valueFlow

    private val _processFailureFlow: MutableSharedFlow<FailedMeasurement> =
        MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = failureBufferCapacity.toInt32(),
            onBufferOverflow = BufferOverflow.SUSPEND
    )
    override val processFailureFlow: SharedFlow<FailedMeasurement>
        get() = _processFailureFlow

    public override val isTransceiving: Trackable<Boolean>
        get() = parentGate.isTransceiving

    public override val isFinalized: Boolean
        get() = parentGate.isFinalized

    public override fun stopTransceiving() {
        parentGate.stopTransceiving()
    }

    public override fun finalize() {
        parentGate.finalize()
    }

    protected abstract suspend fun transformInput(input: ParentT): Result<T, ProcessFailure>

    protected open suspend fun transformationFailure(failure: FailedMeasurement) {
        _processFailureFlow.emit(failure)
    }

    /**
     * This isn't called in init because it is possible that parentGate could be uninitialized at the time it's used
     * in this function. It should be called in the constructor of the first class in the chain that has a
     * final parentGate.
     */
    protected fun initCoroutines() {
        launch {
            parentValueFlow.collect { measurement ->
                when (val convertedResult = transformInput(measurement.value)) {
                    is OK -> update(convertedResult.value at measurement.instant)
                    is Failure -> transformationFailure(convertedResult.error at measurement.instant)
                }
            }
        }

        val parentGate = this.parentGate
        if (parentGate is AcquireChannel<*>) {
            parentGate.processFailureHandler {
                transformationFailure(it)
            }
        }
    }

    private suspend fun update(updated: ValueInstant<T>) {
        _valueFlow.emit(updated)
    }

    public fun createResultFlowIn(
        scope: CoroutineScope
    ): Flow<ProcessResult<T>> = transformEitherIn(
        scope, valueFlow, processFailureFlow,
        transformA = {
            OK(it)
        },
        transformB = {
            Failure(it)
        }
    )

    public companion object {
        public const val DEFAULT_VALUE_BUFFER_CAPACITY: UInt32 = 64u
        public const val DEFAULT_FAILURE_BUFFER_CAPACITY: UInt32 = 8u
    }
}

public abstract class ProcessedAcquireChannel<T : DaqcData, ParentT : DaqcData>(
    valueBufferCapacity: UInt32 = DEFAULT_VALUE_BUFFER_CAPACITY,
    failureBufferCapacity: UInt32 = DEFAULT_FAILURE_BUFFER_CAPACITY
) : ProcessedAcquireGate<T, ParentT>(valueBufferCapacity, failureBufferCapacity) {
    protected abstract override val parentGate: AcquireChannel<ParentT>

    override val parentValueFlow: SharedFlow<ValueInstant<ParentT>> get() = parentGate.valueFlow

    public final override fun startSampling() {
        parentGate.startSampling()
    }

}