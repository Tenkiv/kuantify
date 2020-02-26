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

package org.tenkiv.kuantify.recording.bigstorage

import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.recording.*

public typealias BigStorageHandlerCreator<DT, GT> = (Recorder<DT, GT>) -> BigStorageHandler<DT, GT>

public abstract class BigStorageHandler<DT : DaqcData, GT : DaqcGate<DT>>(
    protected val recorder: Recorder<DT, GT>,
    protected val serializer: KSerializer<DT>
) : CoroutineScope by recorder {
    protected val gate: DaqcGate<DT> get() = recorder.gate
    protected val storageFrequency: StorageFrequency get() = recorder.storageFrequency
    protected val storageLength: StorageLength = requireNotNull(recorder.bigStorageLength)

    public abstract suspend fun getData(filter: StorageFilter<DT>): List<ValueInstant<DT>>

    public abstract suspend fun recordUpdate(update: ValueInstant<DT>)

    /**
     * If there is any special shutdown code that needs to be run when the owning [Recorder] is canceled put it here.
     */
    public abstract suspend fun cancel(shouldDeleteData: Boolean)

}