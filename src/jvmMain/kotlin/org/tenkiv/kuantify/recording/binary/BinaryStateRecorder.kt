/*
 * Copyright 2019 Tenkiv, Inc.
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

package org.tenkiv.kuantify.recording.binary

import kotlinx.coroutines.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.recording.*

//TODO: This file shouldn't need to be in a separate package from recording pending changes to kotlin's method signature
// conflict resolution

public fun <U : Trackable<BinaryStateMeasurement>> CoroutineScope.Recorder(
    updatable: U,
    storageFrequency: StorageFrequency = StorageFrequency.All,
    memoryDuration: StorageDuration = StorageDuration.For(Recorder.memoryDurationDefault),
    diskDuration: StorageDuration = StorageDuration.None,
    filterOnRecord: RecordingFilter<BinaryState, U> = { true }
): Recorder<BinaryState, U> = Recorder(
    scope = this,
    updatable = updatable,
    storageFrequency = storageFrequency,
    memoryDuration = memoryDuration,
    diskDuration = diskDuration,
    filterOnRecord = filterOnRecord,
    valueSerializer = { "\"$it\"" },
    valueDeserializer = BinaryState.Companion::fromString
)

public fun <U : Trackable<BinaryStateMeasurement>> CoroutineScope.Recorder(
    updatable: U,
    storageFrequency: StorageFrequency = StorageFrequency.All,
    numSamplesMemory: StorageSamples = StorageSamples.Number(100),
    numSamplesDisk: StorageSamples = StorageSamples.None,
    filterOnRecord: RecordingFilter<BinaryState, U> = { true }
): Recorder<BinaryState, U> = Recorder(
    scope = this,
    updatable = updatable,
    storageFrequency = storageFrequency,
    numSamplesMemory = numSamplesMemory,
    numSamplesDisk = numSamplesDisk,
    filterOnRecord = filterOnRecord,
    valueSerializer = { "\"$it\"" },
    valueDeserializer = BinaryState.Companion::fromString
)