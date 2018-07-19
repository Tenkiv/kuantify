package org.tenkiv.daqc.recording.quantity

import org.tenkiv.daqc.*
import org.tenkiv.daqc.recording.RecordedUpdatable
import org.tenkiv.daqc.recording.Recorder
import org.tenkiv.daqc.recording.StorageDuration
import org.tenkiv.daqc.recording.StorageFrequency
import javax.measure.Quantity

//TODO: This file shouldn't need to be in a separate package from recording pending changes to kotlin's method signature
// conflict resolution

typealias RecordedQuantityInput<Q> = RecordedUpdatable<DaqcQuantity<Q>, QuantityInput<Q>>
typealias RecordedQuantityOutput<Q> = RecordedUpdatable<DaqcQuantity<Q>, QuantityOutput<Q>>

//TODO: Using type aliases here seems to crash the compiler, switch to type alias when that is fixed.
inline fun <reified Q : Quantity<Q>, U : Updatable<QuantityMeasurement<Q>>>
        U.pairWithNewRecorder(
    storageFrequency: StorageFrequency = StorageFrequency.All,
    memoryDuration: StorageDuration = StorageDuration.For(Recorder.memoryDurationDefault),
    diskDuration: StorageDuration = StorageDuration.Forever,
    noinline filterOnRecord: Recorder<DaqcQuantity<Q>>.(QuantityMeasurement<Q>) -> Boolean = { true }
) =
    RecordedUpdatable(
        this,
        Recorder(
            this,
            storageFrequency,
            memoryDuration,
            diskDuration,
            filterOnRecord,
            valueSerializer = { "\"$it\"" },
            valueDeserializer = DaqcQuantity.Companion::fromString
        )
    )