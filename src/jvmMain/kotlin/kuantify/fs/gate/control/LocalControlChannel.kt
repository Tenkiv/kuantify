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

package kuantify.fs.gate.control

import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.serialization.*
import kuantify.data.*
import kuantify.fs.gate.*
import kuantify.fs.networking.*
import kuantify.gate.control.*
import kuantify.gate.control.output.*
import kuantify.lib.*
import kuantify.networking.configuration.*
import org.tenkiv.coral.*
import physikal.*

public abstract class LocalControlChannel<T : DaqcData>(
    uid: String,
    valueBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER
) : LocalDaqcGate(uid), FSControlChannel<T> {
    private val _valueFlow: MutableSharedFlow<ValueInstant<T>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = valueBufferCapacity.toInt32(),
            onBufferOverflow = BufferOverflow.SUSPEND
        )
    final override val valueFlow: SharedFlow<ValueInstant<T>> get() = _valueFlow

    /**
     * Updates the value. Specifies the [Instant] at which the value was gathered.
     */
    protected suspend fun valueUpdate(value: T, instant: Instant = Clock.System.now()) {
        _valueFlow.emit(value at instant)
    }

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(ValueInstantSerializer(valueSerializer()), RC.VALUE) {
                send(source = valueFlow)
            }
            bindFS(valueSerializer(), RC.CONTROL_SETTING) {
                receive { setting ->
                    setOutput(setting)
                }
            }
        }
    }

}

public abstract class LocalQuantityOutput<QT : Quantity<QT>>(
    uid: String,
    valueBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER
) : LocalControlChannel<DaqcQuantity<QT>>(uid, valueBufferCapacity), QuantityOutput<QT> {

    override fun valueSerializer(): KSerializer<DaqcQuantity<QT>> = DaqcQuantity.serializer()

}

public abstract class LocalBinaryStateOutput(
    uid: String,
    valueBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER
) : LocalControlChannel<BinaryState>(uid, valueBufferCapacity), BinaryStateOutput {

    override fun valueSerializer(): KSerializer<BinaryState> = BinaryState.serializer()

}