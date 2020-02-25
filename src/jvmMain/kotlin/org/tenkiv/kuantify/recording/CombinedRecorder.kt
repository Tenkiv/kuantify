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
import org.tenkiv.kuantify.recording.bigstorage.*
import java.time.*
import kotlin.coroutines.*

public class CombinedRecorder<DT : DaqcData, GT : DaqcGate<DT>> : Recorder<DT, GT> {
    public override val coroutineContext: CoroutineContext
    public override val gate: GT
    public override val storageFrequency: StorageFrequency
    public override val memoryStorageLength: StorageLength
    public override val bigStorageLength: StorageLength

    private val memoryHandler: MemoryHandler<DT>
    private val bigStorageHandler: BigStorageHandler<DT, GT>

    internal constructor(
        scope: CoroutineScope,
        gate: GT,
        storageFrequency: StorageFrequency,
        memoryStorageDuration: StorageDuration,
        bigStorageDuration: StorageDuration,
        bigStorageHandlerCreator: BigStorageHandlerCreator<DT, GT>,
        filterOnRecord: RecordingFilter<DT, GT>
    ) {
        this.coroutineContext = scope.coroutineContext + Job(scope.coroutineContext[Job])
        this.gate = gate
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = memoryStorageDuration
        this.bigStorageLength = bigStorageDuration
        this.memoryHandler = MemoryHandler(this, memoryStorageDuration)
        this.bigStorageHandler = bigStorageHandlerCreator(this)

        createRecordJob(memoryHandler, bigStorageHandler, filterOnRecord)
    }

    internal constructor(
        scope: CoroutineScope,
        gate: GT,
        storageFrequency: StorageFrequency,
        numSamplesMemory: StorageSamples,
        numSamplesBigStorage: StorageSamples,
        bigStorageHandlerCreator: BigStorageHandlerCreator<DT, GT>,
        filterOnRecord: RecordingFilter<DT, GT>
    ) {
        this.coroutineContext = scope.coroutineContext + Job(scope.coroutineContext[Job])
        this.gate = gate
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = numSamplesMemory
        this.bigStorageLength = numSamplesBigStorage
        this.memoryHandler = MemoryHandler(this, numSamplesMemory)
        this.bigStorageHandler = bigStorageHandlerCreator(this)

        createRecordJob(memoryHandler, bigStorageHandler, filterOnRecord)
    }

    public override fun getDataInMemory(): List<ValueInstant<DT>> = memoryHandler.getData()

    public override suspend fun getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<DT>> {
        val filterFun: StorageFilter<DT> = { it.instant in instantRange }
        val dataInMemory = memoryHandler.getData()
        val oldestRequested = instantRange.start
        val oldestMemory = dataInMemory.firstOrNull()

        return if (oldestMemory != null && oldestMemory.instant.isBefore(oldestRequested)) {
            dataInMemory.filter(filterFun)
        } else if (bigStorageLength > memoryStorageLength) {
            bigStorageHandler.getData(filterFun)
        } else {
            dataInMemory.filter(filterFun)
        }
    }

    public override suspend fun getAllData(): List<ValueInstant<DT>> =
        if (memoryStorageLength >= bigStorageLength) {
            getDataInMemory()
        } else {
            bigStorageHandler.getData { true }
        }

    public override suspend fun cancel(deleteBigStorage: Boolean) {
        bigStorageHandler.cancel(deleteBigStorage)
        coroutineContext.cancel()
    }

    // This is only safe to use when comparing memory storage length to big storage length because the constructor
    // overloads of the CombinedRecorder class enforce that they will always be the same type.
    private operator fun StorageLength.compareTo(other: StorageLength): Int = when (this) {
        is StorageSamples -> this.compareTo(other as StorageSamples)
        is StorageDuration -> this.compareTo(other as StorageDuration)
    }

}