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
import org.tenkiv.kuantify.recording.bigstorage.*
import java.time.*

public class BigStorageRecorder<DataT : DaqcData, ChannelT : DaqcChannel<DataT>> internal constructor(
    scope: CoroutineScope,
    public override val gate: ChannelT,
    public override val storageFrequency: StorageFrequency,
    public override val bigStorageLength: StorageLength,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DataT, ChannelT>,
    filterOnRecord: RecordingFilter<DataT, ChannelT>
) : Recorder<DataT, ChannelT>, CoroutineScope by scope.withNewChildJob() {
    private val bigStorageHandler = bigStorageHandlerCreator(this)

    public override val memoryStorageLength: StorageLength? get() = null

    init {
        createRecordJob(memoryHandler = null, bigStorageHandler = bigStorageHandler, filterOnRecord = filterOnRecord)
    }

    public override fun getDataInMemory(): List<ValueInstant<DataT>> = emptyList()

    public override suspend fun getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<DataT>> =
        bigStorageHandler.getData { it.instant in instantRange }

    public override suspend fun getAllData(): List<ValueInstant<DataT>> = bigStorageHandler.getData { true }

    public override suspend fun cancel(deleteBigStorage: Boolean) {
        bigStorageHandler.cancel(deleteBigStorage)
        coroutineContext.cancel()
    }
}