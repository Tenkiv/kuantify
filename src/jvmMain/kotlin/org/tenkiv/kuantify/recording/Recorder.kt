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
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.time.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.recording.bigstorage.*
import java.time.*

public typealias RecordingFilter<DT, GT> = Recorder<DT, GT>.(ValueInstant<DT>) -> Boolean
internal typealias StorageFilter<DT> = (ValueInstant<DT>) -> Boolean

public fun <DT : DaqcData, GT : DaqcGate<DT>> CoroutineScope.Recorder(
    gate: GT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageLength,
    filterOnRecord: RecordingFilter<DT, GT> = { true }
): MemoryRecorder<DT, GT> = MemoryRecorder(this, gate, storageFrequency, memoryStorageLength, filterOnRecord)

public fun <DT : DaqcData, GT : DaqcGate<DT>> CoroutineScope.Recorder(
    gate: GT,
    storageFrequency: StorageFrequency,
    bigStorageLength: StorageLength,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DT, GT>,
    filterOnRecord: RecordingFilter<DT, GT> = { true }
): BigStorageRecorder<DT, GT> =
    BigStorageRecorder(this, gate, storageFrequency, bigStorageLength, bigStorageHandlerCreator, filterOnRecord)

public fun <DT : DaqcData, GT : DaqcGate<DT>> CoroutineScope.Recorder(
    gate: GT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageSamples,
    bigStorageLength: StorageSamples,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DT, GT>,
    filterOnRecord: RecordingFilter<DT, GT> = { true }
): CombinedRecorder<DT, GT> = CombinedRecorder(
    this,
    gate,
    storageFrequency,
    memoryStorageLength,
    bigStorageLength,
    bigStorageHandlerCreator,
    filterOnRecord
)

public fun <DT : DaqcData, GT : DaqcGate<DT>> CoroutineScope.Recorder(
    gate: GT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageDuration,
    bigStorageLength: StorageDuration,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DT, GT>,
    filterOnRecord: RecordingFilter<DT, GT> = { true }
): CombinedRecorder<DT, GT> = CombinedRecorder(
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
    public data class PerNumMeasurements(val number: Int) : StorageFrequency()
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
    public data class Number(val numSamples: Int) : StorageSamples() {

        public override fun compareTo(other: StorageSamples): Int =
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

        public override fun compareTo(other: StorageDuration): Int =
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

public interface Recorder<out DT : DaqcData, out GT : DaqcGate<DT>> : CoroutineScope {
    public val gate: GT
    public val storageFrequency: StorageFrequency
    public val memoryStorageLength: StorageLength?
    public val bigStorageLength: StorageLength?

    public fun getDataInMemory(): List<ValueInstant<DT>>

    public suspend fun getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<DT>>

    public suspend fun getAllData(): List<ValueInstant<DT>>

    public suspend fun cancel(deleteBigStorage: Boolean = false)
}

internal fun <DT : DaqcData, GT : DaqcGate<DT>> Recorder<DT, GT>.createRecordJob(
    memoryHandler: MemoryHandler<DT>?,
    bigStorageHandler: BigStorageHandler<DT, GT>?,
    filterOnRecord: RecordingFilter<DT, GT>
) = launch {
    when (val storageFrequency = storageFrequency) {
        StorageFrequency.All -> gate.updateBroadcaster.openSubscription().consumeEach { update ->
            recordUpdate(update, memoryHandler, bigStorageHandler, filterOnRecord)
        }
        is StorageFrequency.Interval -> while (isActive) {
            delay(storageFrequency.interval)
            recordUpdate(gate.getValue(), memoryHandler, bigStorageHandler, filterOnRecord)
        }
        is StorageFrequency.PerNumMeasurements -> gate.updateBroadcaster.openSubscription().consumeEach { update ->
            var numUnstoredMeasurements = 0
            numUnstoredMeasurements++
            if (numUnstoredMeasurements == storageFrequency.number) {
                recordUpdate(update, memoryHandler, bigStorageHandler, filterOnRecord)
                numUnstoredMeasurements = 0
            }
        }
    }
}

private suspend fun <DT : DaqcData, GT : DaqcGate<DT>> Recorder<DT, GT>.recordUpdate(
    update: ValueInstant<DT>,
    memoryHandler: MemoryHandler<DT>?,
    bigStorageHandler: BigStorageHandler<DT, GT>?,
    filter: RecordingFilter<DT, GT>
) {
    if (filter(update)) {
        memoryHandler?.recordUpdate(update)
        bigStorageHandler?.recordUpdate(update)
        memoryHandler?.cleanMemory()
    }
}