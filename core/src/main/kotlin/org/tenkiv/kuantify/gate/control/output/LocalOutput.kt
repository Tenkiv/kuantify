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

package org.tenkiv.kuantify.gate.control.output

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.*
import kotlin.reflect.*

interface LocalOutput<T : DaqcValue> : LocalControlGate<T>, Output<T> {

    override fun sideRouting(route: SideNetworkRoute) {
        super.sideRouting(route)

        route.add {
            route<Boolean>(RC.IS_TRANSCEIVING, isFullyBiDirectional = false) {
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

interface LocalQuantityOutput<Q : Quantity<Q>> : LocalOutput<DaqcQuantity<Q>>, QuantityOutput<Q> {

    val quantityType: KClass<Q>

    override fun sideRouting(route: SideNetworkRoute) {
        super.sideRouting(route)

        route.add {
            route<QuantityMeasurement<Q>>(RC.VALUE, isFullyBiDirectional = true) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(ComparableQuantitySerializer), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val value = Json.parse(ValueInstantSerializer(ComparableQuantitySerializer), it).value
                    val setting = value.asType<Q>(quantityType.java).toDaqc()

                    setOutput(setting)
                }
            }
        }
    }
}

interface LocalBinaryStateOutput : LocalOutput<BinaryState>, BinaryStateOutput {

    override fun sideRouting(route: SideNetworkRoute) {
        super.sideRouting(route)

        route.add {
            route<BinaryStateMeasurement>(RC.VALUE, isFullyBiDirectional = true) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(BinaryState.serializer()), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val setting = Json.parse(ValueInstantSerializer(BinaryState.serializer()), it).value

                    setOutput(setting)
                }
            }
        }
    }
}