/*
 * Copyright 2018 Tenkiv, Inc.
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

package org.tenkiv.daqc.recording.binary

import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryStateMeasurement
import org.tenkiv.daqc.Updatable
import org.tenkiv.daqc.data.BinaryState
import org.tenkiv.daqc.gate.acquire.input.BinaryStateInput
import org.tenkiv.daqc.gate.control.output.BinaryStateOutput
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