package org.tenkiv.daqc.recording.binary

import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.*
import org.tenkiv.daqc.BinaryState.Companion.fromString
import org.tenkiv.daqc.recording.RecordedUpdatable
import org.tenkiv.daqc.recording.Recorder
import org.tenkiv.daqc.recording.StorageDuration
import org.tenkiv.daqc.recording.StorageFrequency

//TODO: This file shouldn't need to be in a separate package from recording pending changes to kotlin's method signature
// conflict resolution

typealias RecordedBinaryStateInput = RecordedUpdatable<BinaryState, BinaryStateInput>
typealias RecordedBinaryStateOutput = RecordedUpdatable<BinaryState, BinaryStateOutput>

fun <U : Updatable<BinaryStateMeasurement>> U.pairWithNewRecorder(
    storageFrequency: StorageFrequency = StorageFrequency.All,
    memoryDuration: StorageDuration = StorageDuration.For(Recorder.memoryDurationDefault),
    diskDuration: StorageDuration = StorageDuration.Forever,
    filterOnRecord: Recorder<BinaryState>.(ValueInstant<BinaryState>) -> Boolean = { true }
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
            valueDeserializer = BinaryState.Companion::fromString
        )
    )