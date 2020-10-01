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

package kuantify.recording

import kotlinx.coroutines.*
import kuantify.data.*
import kuantify.lib.*

internal class MemoryHandler<DataT : DaqcData>(
    scope: CoroutineScope,
    private val storageLength: StorageLength
) : CoroutineScope by scope {

    private val _dataInMemory = ArrayList<ValueInstant<DataT>>()

    //TODO: Make this return truly immutable list.
    fun getData(): List<ValueInstant<DataT>> = ArrayList(_dataInMemory)

    fun recordUpdate(update: ValueInstant<DataT>) {
        _dataInMemory += update
    }

    fun cleanMemory() {
        if (storageLength is StorageDuration.For) {
            val iterator = _dataInMemory.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().instant.isOlderThan(storageLength.duration)) {
                    iterator.remove()
                } else {
                    break
                }
            }
        }
        if (storageLength is StorageSamples.Number) {
            if (_dataInMemory.size > storageLength.numSamples) _dataInMemory.remove(_dataInMemory.first())
        }
    }

}