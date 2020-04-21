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

package org.tenkiv.kuantify.fs.gate

import kotlinx.coroutines.channels.*
import kotlinx.serialization.builtins.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.kuantify.trackable.*
import physikal.*
import physikal.types.*

public abstract class LocalDigitalGate(uid: String) : LocalDaqcGate(uid), DigitalGate {
    public override val avgPeriod: UpdatableQuantity<Time> = Updatable()

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bind<Quantity<Time>>(RC.AVG_PERIOD) {
                send(source = avgPeriod.openSubscription()) {
                    Serialization.json.stringify(Quantity.serializer(), it)
                }
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    val value = Serialization.json.parse(Quantity.serializer<Time>(), it)
                    avgPeriod.set(value)
                }
            }
            bind<Boolean>(RC.IS_TRANSCEIVING_BIN_STATE) {
                send(source = isTransceivingBinaryState.openSubscription()) {
                    Serialization.json.stringify(Boolean.serializer(), it)
                }
            }
            bind<Boolean>(RC.IS_TRANSCEIVING_FREQUENCY) {
                send(source = isTransceivingFrequency.openSubscription()) {
                    Serialization.json.stringify(Boolean.serializer(), it)
                }
            }
            bind<Boolean>(RC.IS_TRANSCEIVING_PWM) {
                send(source = isTransceivingPwm.openSubscription()) {
                    Serialization.json.stringify(Boolean.serializer(), it)
                }
            }
        }
    }

}

public abstract class FSRemoteDigitalGate(uid: String) : FSRemoteDaqcGate(uid), DigitalGate {
    private val _avgPeriod = RemoteSyncUpdatable<Quantity<Time>>()
    public override val avgPeriod: UpdatableQuantity<Time> get() = _avgPeriod
    
    private val _isTransceivingBinaryState = Updatable<Boolean>()
    public override val isTransceivingBinaryState: Trackable<Boolean>
        get() = _isTransceivingBinaryState

    private val _isTransceivingPwm = Updatable<Boolean>()
    public override val isTransceivingPwm: Trackable<Boolean>
        get() = _isTransceivingPwm

    private val _isTransceivingFrequency = Updatable<Boolean>()
    override val isTransceivingFrequency: Trackable<Boolean>
        get() = _isTransceivingFrequency

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bind<Quantity<Time>>(RC.AVG_PERIOD) {
                send(source = _avgPeriod.localSetChannel) {
                    Serialization.json.stringify(Quantity.serializer(), it)
                }
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    val value = Serialization.json.parse(Quantity.serializer<Time>(), it)
                    avgPeriod.set(value)
                }
            }
            bind<Boolean>(RC.IS_TRANSCEIVING_BIN_STATE) {
                receive {
                    val value = Serialization.json.parse(Boolean.serializer(), it)
                    _isTransceivingBinaryState.set(value)
                }
            }
            bind<Boolean>(RC.IS_TRANSCEIVING_FREQUENCY) {
                receive {
                    val value = Serialization.json.parse(Boolean.serializer(), it)
                    _isTransceivingFrequency.set(value)
                }
            }
            bind<Boolean>(RC.IS_TRANSCEIVING_PWM) {
                receive {
                    val value = Serialization.json.parse(Boolean.serializer(), it)
                    _isTransceivingPwm.set(value)
                }
            }
        }
    }

}