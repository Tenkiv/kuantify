package org.tenkiv.daqc.recording

import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.Updatable
import java.time.Duration

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