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

package org.tenkiv.kuantify.gate.control

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.trackable.*

public abstract class ProcessedControlGate<T : DaqcData, ParentT: DaqcData>(
    initialSetting: ValueInstant<T>? = null
) : ControlChannel<T> {
    protected abstract val parentGate: DaqcGate

    @Volatile
    private var _valueOrNull: ValueInstant<T>? = initialSetting
    public final override val valueOrNull: ValueInstant<T>?
        get() = _valueOrNull

    private val broadcastChannel = BroadcastChannel<ValueInstant<T>>(capacity = Channel.BUFFERED)

    public override val isTransceiving: Trackable<Boolean>
        get() = parentGate.isTransceiving

    public override val isFinalized: Boolean
        get() = parentGate.isFinalized

    public override fun setOutputIfViable(setting: T): SettingViability =
        when(val result = transformToParentType(setting)) {
            is Result.Success -> setParentOutput(result.value)
            is Result.Failure -> unviableSetting(result.error)
        }

    public override fun openSubscription(): ReceiveChannel<ValueInstant<T>> = broadcastChannel.openSubscription()

    public override fun stopTransceiving() {
        parentGate.stopTransceiving()
    }

    public override fun finalize() {
        parentGate.finalize()
    }

    protected abstract fun setParentOutput(setting: ParentT): SettingViability

    protected abstract fun transformToParentType(setting: T): Result<ParentT, SettingProblem>

    protected abstract fun transformFromParentType(value: ParentT): T

    protected abstract fun openParentSubscription(): ReceiveChannel<ValueInstant<ParentT>>

    protected open fun unviableSetting(settingProblem: SettingProblem): SettingViability.Unviable =
        SettingViability.Unviable(settingProblem)

    protected fun initCoroutines() {
        launch {
            openParentSubscription().consumingOnEach {
                update(transformFromParentType(it.value) at it.instant)
            }
        }
    }

    private suspend fun update(updated: ValueInstant<T>) {
        _valueOrNull = updated
        broadcastChannel.send(updated)
    }
}

public abstract class ProcessedControlChannel<T : DaqcData, ParentT: DaqcData>(
    initialSetting: ValueInstant<T>? = null
): ProcessedControlGate<T, ParentT>(initialSetting) {
    protected abstract override val parentGate: ControlChannel<ParentT>

    protected final override fun setParentOutput(setting: ParentT): SettingViability =
        parentGate.setOutputIfViable(setting)

    protected final override fun openParentSubscription(): ReceiveChannel<ValueInstant<ParentT>> =
        parentGate.openSubscription()
}