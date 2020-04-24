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

package org.tenkiv.kuantify.fs.gate.control

import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import physikal.*

private val logger = KotlinLogging.logger {}

public sealed class FSRemoteOutput<T : DaqcValue>(uid: String) : FSRemoteDaqcGate(uid), Output<T> {
    internal val broadcastChannel =
        BroadcastChannel<ValueInstant<T>>(capacity = Channel.BUFFERED)
    internal val settingChannel = Channel<T>(capacity = Channel.CONFLATED)

    public override fun setOutputIfViable(setting: T): SettingViability {
        command { settingChannel.offer(setting) }
        return SettingViability.Viable
    }

}

public abstract class FSRemoteQuantityOutput<QT : Quantity<QT>>(
    uid: String
) : FSRemoteOutput<DaqcQuantity<QT>>(uid), QuantityOutput<QT> {
    @Volatile
    private var _valueOrNull: ValueInstant<DaqcQuantity<QT>>? = null
    public override val valueOrNull: ValueInstant<DaqcQuantity<QT>>?
        get() = _valueOrNull

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS<QuantityMeasurement<QT>>(QuantityMeasurement.quantitySerializer(), RC.VALUE) {
                receive {
                    _valueOrNull = it
                    broadcastChannel.send(it)
                }
            }
            bindFS<Quantity<QT>>(Quantity.serializer(), RC.CONTROL_SETTING) {
                send(source = settingChannel)
            }
        }
    }

}

public abstract class FSRemoteBinaryStateOutput(uid: String) :
    FSRemoteOutput<BinaryState>(uid), BinaryStateOutput {
    @Volatile
    private var _valueOrNull: ValueInstant<BinaryState>? = null
    public override val valueOrNull: ValueInstant<BinaryState>?
        get() = _valueOrNull

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(BinaryStateMeasurement.binaryStateSerializer(), RC.VALUE) {
                receive {
                    _valueOrNull = it
                    broadcastChannel.send(it)
                }
            }
            bindFS(BinaryState.serializer(), RC.CONTROL_SETTING) {
                send(source = settingChannel)
            }
        }
    }

}