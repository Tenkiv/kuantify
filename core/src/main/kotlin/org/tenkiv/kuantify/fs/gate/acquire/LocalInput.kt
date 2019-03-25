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
 *
 */

package org.tenkiv.kuantify.fs.gate.acquire

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.*

public interface LocalInput<T : DaqcValue, out D : LocalDevice> : LocalAcquireGate<T, D>, Input<T> {

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

public interface LocalQuantityInput<Q : Quantity<Q>, out D : LocalDevice> : LocalInput<DaqcQuantity<Q>, D>,
    QuantityInput<Q> {

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<QuantityMeasurement<Q>>(RC.VALUE) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(ComparableQuantitySerializer), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}

public interface LocalBinaryStateInput<out D : LocalDevice> : LocalInput<BinaryState, D>,
    BinaryStateInput {

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<BinaryStateMeasurement>(RC.VALUE) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(BinaryState.serializer()), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}