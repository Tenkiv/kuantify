package org.tenkiv.daqc.recording

import org.tenkiv.BinaryStateMeasurement
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.channel.BinaryStateInput
import org.tenkiv.daqc.hardware.definitions.channel.BinaryStateOutput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityInput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityOutput
import java.time.Duration
import java.time.Instant
import javax.measure.Quantity


typealias RecordedQuantityInput<Q> = RecordedUpdatable<DaqcQuantity<Q>, QuantityInput<Q>>
typealias RecordedBinaryStateInput = RecordedUpdatable<BinaryState, BinaryStateInput>
typealias RecordedQuantityOutput<Q> = RecordedUpdatable<DaqcQuantity<Q>, QuantityOutput<Q>>
typealias RecordedBinaryStateOutput = RecordedUpdatable<BinaryState, BinaryStateOutput>

//TODO: Move default parameter values in recorder creation function to constants
fun <T> Updatable<ValueInstant<T>>.newRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever,
        filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true },
        valueSerializer: (T) -> String,
        valueDeserializer: (String) -> T
): Recorder<T> =
        Recorder(this,
                storageFrequency,
                memoryDuration,
                diskDuration,
                filterOnRecord,
                valueSerializer,
                valueDeserializer)

fun Updatable<BinaryStateMeasurement>.newBinaryStateRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever,
        filterOnRecord: Recorder<BinaryState>.(ValueInstant<BinaryState>) -> Boolean = { true }
) =
        Recorder(this,
                storageFrequency,
                memoryDuration,
                diskDuration,
                filterOnRecord,
                valueSerializer = BinaryState::toString,
                valueDeserializer = BinaryState.Companion::fromString)

//TODO: Add optional filterOnRecord parameter when supported by kotlin
inline fun <reified Q : Quantity<Q>> Updatable<QuantityMeasurement<Q>>.newQuantityRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever
): Recorder<DaqcQuantity<Q>> =
        newRecorder(storageFrequency,
                memoryDuration,
                diskDuration,
                filterOnRecord = { true },
                valueSerializer = DaqcQuantity<Q>::toString,
                valueDeserializer = DaqcQuantity.Companion::fromString)

fun <T, U : Updatable<ValueInstant<T>>> U.pairWithNewRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever,
        filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true },
        valueSerializer: (T) -> String,
        valueDeserializer: (String) -> T
) =
        RecordedUpdatable(
                this,
                this.newRecorder(storageFrequency,
                        memoryDuration,
                        diskDuration,
                        filterOnRecord,
                        valueSerializer,
                        valueDeserializer)
        )

fun <U : Updatable<BinaryStateMeasurement>> U.pairWithNewBinStateRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever,
        filterOnRecord: Recorder<BinaryState>.(ValueInstant<BinaryState>) -> Boolean = { true }
) =
        RecordedUpdatable(
                this,
                newBinaryStateRecorder(storageFrequency,
                        memoryDuration,
                        diskDuration,
                        filterOnRecord)
        )

//TODO: Add optional filterOnRecord parameter when supported by kotlin
//TODO: Using type aliases here seems to crash the compiler, switch to type alias when that is fixed.
inline fun <reified Q : Quantity<Q>, U : Updatable<ValueInstant<DaqcQuantity<Q>>>>
        U.pairWithNewQuantityRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever
) =
        RecordedUpdatable(this, newQuantityRecorder(storageFrequency, memoryDuration, diskDuration))

fun <T> List<ValueInstant<T>>.getDataInRange(instantRange: ClosedRange<Instant>):
        List<ValueInstant<T>> {
    val oldestRequested = instantRange.start
    val newestRequested = instantRange.endInclusive

    return this.filter {
        it.instant.isAfter(oldestRequested) &&
                it.instant.isBefore(newestRequested) ||
                it.instant == oldestRequested ||
                it.instant == newestRequested
    }
}


data class RecordedUpdatable<out T, out U : Updatable<ValueInstant<T>>>(val updatable: U,
                                                                        val recorder: Recorder<T>)

sealed class StorageFrequency {

    object All : StorageFrequency()

    data class Interval(val interval: Duration) : StorageFrequency()

    data class PerNumMeasurements(val number: Int) : StorageFrequency()
}

sealed class StorageDuration : Comparable<StorageDuration> {

    object Forever : StorageDuration() {

        override fun compareTo(other: StorageDuration): Int =
                when (other) {
                    is Forever -> 0
                    else -> 1
                }

    }

    object None : StorageDuration() {

        override fun compareTo(other: StorageDuration): Int =
                when (other) {
                    is None -> 0
                    else -> -1
                }

    }

    data class For(val duration: Duration) : StorageDuration() {

        override fun compareTo(other: StorageDuration): Int =
                when (other) {
                    is Forever -> -1
                    is None -> 1
                    is For -> duration.compareTo(other.duration)
                }

    }

}