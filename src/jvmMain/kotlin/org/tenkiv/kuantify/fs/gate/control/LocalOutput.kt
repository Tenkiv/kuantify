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

package org.tenkiv.kuantify.fs.gate.control

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import physikal.*
import kotlin.reflect.*

public interface LocalOutput<T : DaqcValue, out D : LocalDevice> : LocalControlGate<T, D>, Output<T> {

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<Boolean>(RC.IS_TRANSCEIVING) {
                serializeMessage {
                    Json.stringify(BooleanSerializer, it)
                }

                setLocalUpdateChannel(isTransceiving.updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }

        }
    }
}

public interface LocalQuantityOutput<QT : Quantity<QT>, out D : LocalDevice> : LocalOutput<DaqcQuantity<QT>, D>,
    QuantityOutput<QT> {

    public val quantityType: KClass<QT>

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)
        routing.addToThisPath {
            bind<QuantityMeasurement<QT>>(RC.VALUE) {
                serializeMessage {
                    Serialization.json.stringify(Measurement.quantitySerializer(), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receive {
                    val setting = Serialization.json.parse(Quantity.serializer<QT>(), it)

                    setOutput(setting)
                }
            }
        }
    }

}

public interface LocalBinaryStateOutput<out D : LocalDevice> : LocalOutput<BinaryState, D>, BinaryStateOutput {

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<BinaryStateMeasurement>(RC.VALUE) {
                serializeMessage {
                    Serialization.json.stringify(ValueInstantSerializer(BinaryState.serializer()), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receive {
                    val setting = Serialization.json.parse(BinaryState.serializer(), it)

                    setOutput(setting)
                }
            }
        }
    }
}