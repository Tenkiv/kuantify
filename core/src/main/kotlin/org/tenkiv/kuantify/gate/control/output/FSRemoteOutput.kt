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

import kotlinx.coroutines.channels.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.*
import kotlin.coroutines.*
import kotlin.reflect.*

sealed class FSRemoteOutput<T : DaqcValue>(coroutineContext: CoroutineContext, uid: String) :
    FSRemoteControlGate<T>(coroutineContext, uid), Output<T> {

    internal val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

    override fun setOutput(setting: T, panicOnFailure: Boolean): SettingResult.Success {
        _updateBroadcaster.offer(setting.now())
        return SettingResult.Success
    }

    internal val _isTransceiving = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    override fun sideRouting(routing: SideNetworkRouting) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<Boolean>(RC.IS_TRANSCEIVING, isFullyBiDirectional = false) {
                receiveMessage(NullResolutionStrategy.PANIC) {
                    val value = Json.parse(BooleanSerializer, it)
                    _isTransceiving.value = value
                }
            }
        }
    }
}

abstract class FSRemoteQuantityOutput<Q : Quantity<Q>>(coroutineContext: CoroutineContext, uid: String) :
    FSRemoteOutput<DaqcQuantity<Q>>(coroutineContext, uid), QuantityOutput<Q> {

    abstract val quantityType: KClass<Q>

    override fun sideRouting(routing: SideNetworkRouting) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<QuantityMeasurement<Q>>(RC.VALUE, isFullyBiDirectional = true) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(ComparableQuantitySerializer), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val (value, instant) = Json.parse(ValueInstantSerializer(ComparableQuantitySerializer), it)
                    val setting = value.asType<Q>(quantityType.java).toDaqc()

                    _updateBroadcaster.send(setting at instant)
                }
            }
        }
    }
}

abstract class FSRemoteBinaryStateOutput(coroutineContext: CoroutineContext, uid: String) :
    FSRemoteOutput<BinaryState>(coroutineContext, uid), BinaryStateOutput {

    override fun sideRouting(routing: SideNetworkRouting) {
        super.sideRouting(routing)

        //TODO: This means the time associated with the update will be the time of the update on the local device if
        //  the command came from another remote but if it comes from this one it will be the time at which it was sent
        //  inconsistency is bad.
        routing.addToThisPath {
            bind<BinaryStateMeasurement>(RC.VALUE, isFullyBiDirectional = true) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(BinaryState.serializer()), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val settingInstant = Json.parse(ValueInstantSerializer(BinaryState.serializer()), it)

                    _updateBroadcaster.send(settingInstant)
                }
            }
        }
    }
}