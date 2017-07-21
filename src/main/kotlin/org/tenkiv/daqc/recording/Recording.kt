package org.tenkiv.daqc.recording

import org.tenkiv.BinaryStateMeasurement
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
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
fun <T> recorder(storageFrequency: StorageFrequency = StorageFrequency.All,
                 memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
                 diskDuration: StorageDuration = StorageDuration.Forever,
                 updatable: Updatable<ValueInstant<T>>,
                 dataDeserializer: (String) -> T) = Recorder(storageFrequency,
        memoryDuration,
        diskDuration,
        updatable,
        dataDeserializer)

fun Updatable<BinaryStateMeasurement>.binaryStateRecorder(storageFrequency: StorageFrequency =
                                                          StorageFrequency.All,
                                                          memoryDuration: StorageDuration =
                                                          StorageDuration.For(30L.secondsSpan),
                                                          diskDuration: StorageDuration =
                                                          StorageDuration.Forever) =
        Recorder(storageFrequency,
                memoryDuration,
                diskDuration,
                this) { DaqcValue.binaryFromString(it) }

fun <T> Updatable<ValueInstant<T>>.createRecorder(storageFrequency: StorageFrequency = StorageFrequency.All,
                                                  memoryDuration: StorageDuration =
                                                  StorageDuration.For(30L.secondsSpan),
                                                  diskDuration: StorageDuration = StorageDuration.Forever,
                                                  dataDeserializer: (String) -> T): Recorder<T> =
        Recorder(storageFrequency,
                memoryDuration,
                diskDuration,
                this,
                dataDeserializer)

inline fun <reified Q : Quantity<Q>>
        Updatable<QuantityMeasurement<Q>>.daqcQuantityRecorder(storageFrequency: StorageFrequency =
                                                               StorageFrequency.All,
                                                               memoryDuration: StorageDuration =
                                                               StorageDuration.For(30L.secondsSpan),
                                                               diskDuration: StorageDuration =
                                                               StorageDuration.Forever): Recorder<DaqcQuantity<Q>> =
        recorder(storageFrequency,
                memoryDuration,
                diskDuration,
                this) { DaqcValue.quantityFromString<Q>(it) }

fun <T, U : Updatable<ValueInstant<T>>> U.pairWithNewRecorder(storageFrequency: StorageFrequency = StorageFrequency.All,
                                                              memoryDuration: StorageDuration =
                                                              StorageDuration.For(30L.secondsSpan),
                                                              diskDuration: StorageDuration = StorageDuration.Forever,
                                                              dataDeserializer: (String) -> T) =
        RecordedUpdatable(this, this.createRecorder(storageFrequency, memoryDuration, diskDuration, dataDeserializer))

fun <U : Updatable<BinaryStateMeasurement>> U.pairWithNewBinStateRecorder(storageFrequency: StorageFrequency =
                                                                          StorageFrequency.All,
                                                                          memoryDuration: StorageDuration =
                                                                          StorageDuration.For(30L.secondsSpan),
                                                                          diskDuration: StorageDuration =
                                                                          StorageDuration.Forever) =
        RecordedUpdatable(this, binaryStateRecorder(storageFrequency, memoryDuration, diskDuration))

//TODO: Using type aliases here seems to crash the compiler, switch to type alias when that is fixed.
inline fun <reified Q : Quantity<Q>, U : Updatable<ValueInstant<DaqcQuantity<Q>>>>
        U.pairWithNewQuantityRecorder(storageFrequency: StorageFrequency =
                                      StorageFrequency.All,
                                      memoryDuration: StorageDuration =
                                      StorageDuration.For(30L.secondsSpan),
                                      diskDuration: StorageDuration =
                                      StorageDuration.Forever) =
        RecordedUpdatable(this, daqcQuantityRecorder(storageFrequency, memoryDuration, diskDuration))

fun <T> List<ValueInstant<T>>.getDataInRange(instantRange: ClosedRange<Instant>, shouldBeEncompassing: Boolean = false):
        List<ValueInstant<T>> {

    val start = instantRange.start
    val end = instantRange.endInclusive

    if (shouldBeEncompassing && start.isBefore(Instant.now())) {
        throw IllegalArgumentException("Requested start time is in the future.")
    }

    val found = ArrayList<ValueInstant<T>>(filter { it.instant.isAfter(start) && it.instant.isBefore(end) })

    if (shouldBeEncompassing && found.sortedBy { it.instant }.last().instant.isBefore(end)) {
        throw IllegalArgumentException("Last possible Instant is before last requested Instant")
    }

    return found
}


data class RecordedUpdatable<out T, out U : Updatable<ValueInstant<T>>>(val updatable: U,
                                                                        val recorder: Recorder<T>)

sealed class StorageFrequency {

    object All : StorageFrequency()

    data class Interval(val interval: Duration) : StorageFrequency()

    data class PerNumMeasurements(val number: Int) : StorageFrequency()
}

sealed class StorageDuration {

    object Forever : StorageDuration()

    object None : StorageDuration()

    data class For(val duration: Duration) : StorageDuration()

}