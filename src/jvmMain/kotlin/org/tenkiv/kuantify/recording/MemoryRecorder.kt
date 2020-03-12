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

package org.tenkiv.kuantify.recording

import kotlinx.coroutines.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.*
import java.time.*

public class MemoryRecorder<DT : DaqcData, GT : DaqcGate<DT>> internal constructor(
    scope: CoroutineScope,
    public override val gate: GT,
    public override val storageFrequency: StorageFrequency,
    public override val memoryStorageLength: StorageLength,
    filterOnRecord: RecordingFilter<DT, GT>
) : Recorder<DT, GT>, CoroutineScope by scope.withNewChildJob() {
    private val memoryHandler = MemoryHandler<DT>(this, memoryStorageLength)

    public override val bigStorageLength: StorageLength? get() = null

    init {
        createRecordJob(memoryHandler = memoryHandler, bigStorageHandler = null, filterOnRecord = filterOnRecord)
    }

    public override fun getDataInMemory(): List<ValueInstant<DT>> = memoryHandler.getData()

    public override suspend fun getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<DT>> =
        memoryHandler.getData().filter { it.instant in instantRange }

    public override suspend fun getAllData(): List<ValueInstant<DT>> = getDataInMemory()

    public override suspend fun cancel(deleteBigStorage: Boolean) = cancel()

    public fun cancel() {
        coroutineContext.cancel()
    }

}