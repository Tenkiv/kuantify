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
import kotlinx.coroutines.time.*
import kotlinx.datetime.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.recording.bigstorage.*
import kotlin.time.*

public typealias RecordingFilter<DataT, ChannelT> = Recorder<DataT, ChannelT>.(ValueInstant<DataT>) -> Boolean
internal typealias StorageFilter<DataT> = (ValueInstant<DataT>) -> Boolean

public fun <DataT : DaqcData, ChannelT : DaqcChannel<DataT>> CoroutineScope.Recorder(
    gate: ChannelT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageLength,
    filterOnRecord: RecordingFilter<DataT, ChannelT> = { true }
): MemoryRecorder<DataT, ChannelT> = MemoryRecorder(this, gate, storageFrequency, memoryStorageLength, filterOnRecord)

public fun <DataT : DaqcData, ChannelT : DaqcChannel<DataT>> CoroutineScope.Recorder(
    gate: ChannelT,
    storageFrequency: StorageFrequency,
    bigStorageLength: StorageLength,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DataT, ChannelT>,
    filterOnRecord: RecordingFilter<DataT, ChannelT> = { true }
): BigStorageRecorder<DataT, ChannelT> =
    BigStorageRecorder(this, gate, storageFrequency, bigStorageLength, bigStorageHandlerCreator, filterOnRecord)

public fun <DataT : DaqcData, ChannelT : DaqcChannel<DataT>> CoroutineScope.Recorder(
    gate: ChannelT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageSamples,
    bigStorageLength: StorageSamples,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DataT, ChannelT>,
    filterOnRecord: RecordingFilter<DataT, ChannelT> = { true }
): CombinedRecorder<DataT, ChannelT> = CombinedRecorder(
    this,
    gate,
    storageFrequency,
    memoryStorageLength,
    bigStorageLength,
    bigStorageHandlerCreator,
    filterOnRecord
)

public fun <DataT : DaqcData, ChannelT : DaqcChannel<DataT>> CoroutineScope.Recorder(
    gate: ChannelT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageDuration,
    bigStorageLength: StorageDuration,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DataT, ChannelT>,
    filterOnRecord: RecordingFilter<DataT, ChannelT> = { true }
): CombinedRecorder<DataT, ChannelT> = CombinedRecorder(
    this,
    gate,
    storageFrequency,
    memoryStorageLength,
    bigStorageLength,
    bigStorageHandlerCreator,
    filterOnRecord
)

/**
 * Sealed class denoting the frequency at which samples should be stored.
 */
public sealed class StorageFrequency {

    /**
     * Store all data received.
     */
    public object All : StorageFrequency()

    /**
     * Store the data within an interval.
     *
     * @param interval The [Duration] over which samples will be stored.
     */
    public data class Interval(val interval: Duration) : StorageFrequency()

    /**
     * Store data per number of measurements received.
     *
     * @param number The interval of samples at which to store a new sample.
     */
    public data class PerNumMeasurements(val number: Int32) : StorageFrequency()
}

/**
 * Sealed class for how long data should be stored either in memory or on disk.
 */
public sealed class StorageLength

/**
 * Sealed class denoting the number of samples to be kept in either memory or on disk.
 */
public sealed class StorageSamples : StorageLength(), Comparable<StorageSamples> {

    /**
     * Keep all data unless otherwise noted.
     */
    public object All : StorageSamples() {

        public override fun compareTo(other: StorageSamples): Int =
            when (other) {
                is All -> 0
                else -> 1
            }

    }

    /**
     * Keep a specific number of samples in memory or on disk.
     *
     * @param numSamples The number of samples to keep in memory or on disk.
     */
    public data class Number(val numSamples: Int32) : StorageSamples() {

        public override fun compareTo(other: StorageSamples): Int32 =
            when (other) {
                is All -> -1
                is Number -> numSamples.compareTo(other.numSamples)
            }
    }

}

/**
 * Sealed class denoting the length of time which a sample should be kept in memory or on disk.
 */
public sealed class StorageDuration : StorageLength(), Comparable<StorageDuration> {

    /**
     * Keep the data without respect to time.
     */
    public object Forever : StorageDuration() {

        public override fun compareTo(other: StorageDuration): Int32 =
            when (other) {
                is Forever -> 0
                else -> 1
            }

    }

    /**
     * Keep the data for a specified duration.
     *
     * @param duration The [Duration] with which to keep the data.
     */
    public data class For(val duration: Duration) : StorageDuration() {

        public override fun compareTo(other: StorageDuration): Int =
            when (other) {
                is Forever -> -1
                is For -> duration.compareTo(other.duration)
            }
    }
}

public interface Recorder<out DataT : DaqcData, out ChannelT : DaqcChannel<DataT>> : CoroutineScope {
    public val gate: ChannelT
    public val storageFrequency: StorageFrequency
    public val memoryStorageLength: StorageLength?
    public val bigStorageLength: StorageLength?

    public fun getDataInMemory(): List<ValueInstant<DataT>>

    public suspend fun getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<DataT>>

    public suspend fun getAllData(): List<ValueInstant<DataT>>

    public suspend fun cancel(deleteBigStorage: Boolean = false)
}

internal fun <DataT : DaqcData, ChannelT : DaqcChannel<DataT>> Recorder<DataT, ChannelT>.createRecordJob(
    memoryHandler: MemoryHandler<DataT>?,
    bigStorageHandler: BigStorageHandler<DataT, ChannelT>?,
    filterOnRecord: RecordingFilter<DataT, ChannelT>
) = launch {
    when (val storageFrequency = storageFrequency) {
        StorageFrequency.All -> gate.onEachUpdate { update ->
            recordUpdate(update, memoryHandler, bigStorageHandler, filterOnRecord)
        }
        is StorageFrequency.Interval -> while (isActive) {
            delay(storageFrequency.interval)
            recordUpdate(gate.getValue(), memoryHandler, bigStorageHandler, filterOnRecord)
        }
        is StorageFrequency.PerNumMeasurements -> {
            var numUnstoredMeasurements = 0
            gate.onEachUpdate { update ->
                numUnstoredMeasurements++
                if (numUnstoredMeasurements == storageFrequency.number) {
                    recordUpdate(update, memoryHandler, bigStorageHandler, filterOnRecord)
                    numUnstoredMeasurements = 0
                }
            }
        }
    }
}

private suspend fun <DataT : DaqcData, ChannelT : DaqcChannel<DataT>> Recorder<DataT, ChannelT>.recordUpdate(
    update: ValueInstant<DataT>,
    memoryHandler: MemoryHandler<DataT>?,
    bigStorageHandler: BigStorageHandler<DataT, ChannelT>?,
    filter: RecordingFilter<DataT, ChannelT>
) {
    if (filter(update)) {
        memoryHandler?.recordUpdate(update)
        bigStorageHandler?.recordUpdate(update)
        memoryHandler?.cleanMemory()
    }
}