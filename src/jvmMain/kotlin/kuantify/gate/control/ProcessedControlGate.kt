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

package kuantify.gate.control

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import org.tenkiv.coral.*
import kuantify.data.*
import kuantify.gate.*
import kuantify.lib.*
import kuantify.trackable.*

public abstract class ProcessedControlGate<T : DaqcData, ParentT: DaqcData>(
    valueBufferCapacity: UInt32 = DEFAULT_VALUE_BUFFER_CAPACITY
) : ControlChannel<T> {
    protected abstract val parentGate: DaqcGate
    protected abstract val parentValueFlow: Flow<ValueInstant<ParentT>>

    private val _valueFlow: MutableSharedFlow<ValueInstant<T>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = valueBufferCapacity.toInt32(),
            onBufferOverflow = BufferOverflow.SUSPEND
        )
    override val valueFlow: SharedFlow<ValueInstant<T>> get() = _valueFlow

    public override val isTransceiving: Trackable<Boolean>
        get() = parentGate.isTransceiving

    public override val isFinalized: Boolean
        get() = parentGate.isFinalized

    public override suspend fun setOutputIfViable(setting: T): SettingViability =
        when(val result = transformToParentType(setting)) {
            is Result.OK -> setParentOutput(result.value)
            is Result.Failure -> SettingViability.Unviable(result.error)
        }

    public override fun stopTransceiving() {
        parentGate.stopTransceiving()
    }

    public override fun finalize() {
        parentGate.finalize()
    }

    protected abstract suspend fun setParentOutput(setting: ParentT): SettingViability

    protected abstract fun transformToParentType(setting: T): Result<ParentT, SettingProblem>

    protected abstract fun transformFromParentType(value: ParentT): T

    protected fun initCoroutines() {
        launch {
            parentValueFlow.collect {
                update(transformFromParentType(it.value) at it.instant)
            }
        }
    }

    private suspend fun update(updated: ValueInstant<T>) {
        _valueFlow.emit(updated)
    }

    public companion object {
        public const val DEFAULT_VALUE_BUFFER_CAPACITY: UInt32 = 64u
    }
}

public abstract class ProcessedControlChannel<T : DaqcData, ParentT: DaqcData>(
    valueBufferCapacity: UInt32 = DEFAULT_VALUE_BUFFER_CAPACITY
): ProcessedControlGate<T, ParentT>(valueBufferCapacity) {
    protected abstract override val parentGate: ControlChannel<ParentT>

    override val parentValueFlow: Flow<ValueInstant<ParentT>> get() = parentGate.valueFlow

    protected final override suspend fun setParentOutput(setting: ParentT): SettingViability =
        parentGate.setOutputIfViable(setting)

}