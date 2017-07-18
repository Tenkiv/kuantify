package org.tenkiv.daqc.recording

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

fun <T : DaqcValue> daqcValueRecorder(storageFrequency: StorageFrequency = StorageFrequency.All,
                                      memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
                                      diskDuration: StorageDuration = StorageDuration.Forever,
                                      updatable: Updatable<ValueInstant<T>>) = Recorder(storageFrequency,
        memoryDuration,
        diskDuration,
        updatable) { TODO("Use jackson or parsing here") }

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

fun <T : DaqcValue> Updatable<ValueInstant<T>>.createRecorder(storageFrequency: StorageFrequency =
                                                              StorageFrequency.All,
                                                              memoryDuration: StorageDuration =
                                                              StorageDuration.For(30L.secondsSpan),
                                                              diskDuration: StorageDuration =
                                                              StorageDuration.Forever): Recorder<T> =
        Recorder(storageFrequency,
                memoryDuration,
                diskDuration,
                this) { TODO("Use jackson or parsing here") }

fun <T, U : Updatable<ValueInstant<T>>> U.pairWithNewRecorder(storageFrequency: StorageFrequency = StorageFrequency.All,
                                                              memoryDuration: StorageDuration =
                                                              StorageDuration.For(30L.secondsSpan),
                                                              diskDuration: StorageDuration = StorageDuration.Forever,
                                                              dataDeserializer: (String) -> T) =
        RecordedUpdatable(this, this.createRecorder(storageFrequency, memoryDuration, diskDuration, dataDeserializer))

fun <T : DaqcValue, U : Updatable<ValueInstant<T>>> U.pairWithNewRecorder(storageFrequency: StorageFrequency =
                                                                          StorageFrequency.All,
                                                                          memoryDuration: StorageDuration =
                                                                          StorageDuration.For(30L.secondsSpan),
                                                                          diskDuration: StorageDuration =
                                                                          StorageDuration.Forever) =
        RecordedUpdatable(this, this.createRecorder(storageFrequency, memoryDuration, diskDuration))

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