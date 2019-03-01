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

import kotlinx.coroutines.channels.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import tec.units.indriya.*
import javax.measure.*
import kotlin.coroutines.*
import kotlin.reflect.*

sealed class FSRemoteInput<T : DaqcValue>(coroutineContext: CoroutineContext, uid: String) :
    FSRemoteAcquireGate<T>(coroutineContext, uid), Input<T>,
    NetworkBoundSide {

    internal val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

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

abstract class FSRemoteQuantityInput<Q : Quantity<Q>>(coroutineContext: CoroutineContext, uid: String) :
    FSRemoteInput<DaqcQuantity<Q>>(coroutineContext, uid),
    QuantityInput<Q> {

    abstract val quantityType: KClass<Q>

    private fun unsafeUpdate(measurement: ValueInstant<ComparableQuantity<*>>) {
        val (value, instant) = measurement

        _updateBroadcaster.offer(value.asType(quantityType.java).toDaqc() at instant)
    }

    override fun sideRouting(routing: SideNetworkRouting) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<QuantityMeasurement<Q>>(RC.VALUE, isFullyBiDirectional = false) {
                receiveMessage(NullResolutionStrategy.PANIC) {
                    val measurement = Json.parse(ValueInstantSerializer(ComparableQuantitySerializer), it)
                    unsafeUpdate(measurement)
                }
            }
        }

    }

}

abstract class FSRemoteBinaryStateInput(coroutineContext: CoroutineContext, uid: String) :
    FSRemoteInput<BinaryState>(coroutineContext, uid),
    BinaryStateInput {

    override fun sideRouting(routing: SideNetworkRouting) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<BinaryStateMeasurement>(RC.VALUE, isFullyBiDirectional = false) {
                receiveMessage(NullResolutionStrategy.PANIC) {
                    val measurement = Json.parse(ValueInstantSerializer(BinaryState.serializer()), it)
                    _updateBroadcaster.offer(measurement)
                }
            }
        }

    }
}
