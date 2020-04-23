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

package org.tenkiv.kuantify.fs.gate.acquire

import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import physikal.*

public sealed class FSRemoteInput<T : DaqcValue>(uid: String) : FSRemoteAcquireChannel<T>(uid), Input<T> {
    @Volatile
    internal var _valueOrNull: ValueInstant<T>? = null
    public override val valueOrNull: ValueInstant<T>?
        get() = _valueOrNull

    internal val broadcastChannel =
        BroadcastChannel<ValueInstant<T>>(capacity = Channel.BUFFERED)

    public override fun openSubscription(): ReceiveChannel<ValueInstant<T>> = broadcastChannel.openSubscription()
}

public abstract class FSRemoteQuantityInput<QT : Quantity<QT>>(
    uid: String
) : FSRemoteInput<DaqcQuantity<QT>>(uid), QuantityInput<QT> {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS<QuantityMeasurement<QT>>(QuantityMeasurement.quantitySerializer(), RC.VALUE) {
                receive { measurement ->
                    _valueOrNull = measurement
                    broadcastChannel.send(measurement)
                }
            }
        }
    }

}

public abstract class FSRemoteBinaryStateInput(uid: String) : FSRemoteInput<BinaryState>(uid), BinaryStateInput {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(BinaryStateMeasurement.binaryStateSerializer(), RC.VALUE) {
                receive { measurement ->
                    _valueOrNull = measurement
                    broadcastChannel.send(measurement)
                }
            }
        }
    }

}
