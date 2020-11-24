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

package kuantify.fs.gate.acquire

import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kuantify.*
import kuantify.data.*
import kuantify.fs.gate.*
import kuantify.fs.networking.*
import kuantify.gate.acquire.input.*
import kuantify.hardware.channel.*
import kuantify.lib.*
import kuantify.networking.configuration.*
import org.tenkiv.coral.*
import physikal.*

public abstract class FSRemoteAcquireChannel<T : DaqcData>(
    uid: String,
    valueBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER
) : FSRemoteDaqcGate(uid), FSAcquireChannel<T> {
    private val _valueFlow: MutableSharedFlow<ValueInstant<T>> =
        MutableSharedFlow(
            replay = 1,
            extraBufferCapacity = valueBufferCapacity.toInt32(),
            onBufferOverflow = BufferOverflow.SUSPEND
        )
    final override val valueFlow: SharedFlow<ValueInstant<T>> get() = _valueFlow

    private val startSamplingChannel = Channel<Ping>(Channel.RENDEZVOUS)
    public final override fun startSampling() {
        modifyConfiguration {
            command {
                startSamplingChannel.offer(Ping)
            }
        }
    }

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(ValueInstantSerializer(valueSerializer()), RC.VALUE) {
                receiveDirect { measurement ->
                    _valueFlow.emit(measurement)
                }
            }

            bindPing(RC.START_SAMPLING) {
                send(source = startSamplingChannel)
            }
        }
    }

}

public abstract class FSRemoteQuantityInput<QT : Quantity<QT>>(
    uid: String,
    valueBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER
) : FSRemoteAcquireChannel<DaqcQuantity<QT>>(uid, valueBufferCapacity), QuantityInput<QT> {

    override fun valueSerializer(): KSerializer<DaqcQuantity<QT>> = DaqcQuantity.serializer()

}

public abstract class FSRemoteBinaryStateInput(
    uid: String,
    valueBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER
) : FSRemoteAcquireChannel<BinaryState>(uid, valueBufferCapacity), BinaryStateInput {

    override fun valueSerializer(): KSerializer<BinaryState> = BinaryState.serializer()

}