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

package org.tenkiv.kuantify.fs.hardware.channel

import kotlinx.coroutines.channels.*
import kotlinx.serialization.builtins.*
import org.tenkiv.kuantify.fs.gate.acquire.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.kuantify.trackable.*
import physikal.*

public abstract class LocalAnalogInput<out DeviceT>(uid: String) : LocalQuantityInput<Voltage>(uid), AnalogInput
        where DeviceT : LocalDevice, DeviceT : AnalogDaqDevice {
    public abstract override val device: DeviceT

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(Boolean.serializer(), RC.BUFFER) {
                send(source = buffer.openSubscription())
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    buffer.set(it)
                }
            }
            bindFS<Quantity<Voltage>>(Quantity.serializer(), RC.MAX_ACCEPTABLE_ERROR) {
                send(source = maxAcceptableError.openSubscription())
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    maxAcceptableError.set(it)
                }
            }
            bindFS<Quantity<Voltage>>(Quantity.serializer(), RC.MAX_VOLTAGE) {
                send(source = maxVoltage.openSubscription())
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    maxVoltage.set(it)
                }
            }
        }
    }

}

public abstract class FSRemoteAnalogInput<out DeviceT>(uid: String) : FSRemoteQuantityInput<Voltage>(uid), AnalogInput
        where DeviceT : AnalogDaqDevice, DeviceT : FSRemoteDevice {
    public abstract override val device: DeviceT

    private val _buffer = RemoteSyncUpdatable<Boolean>()
    public override val buffer: Updatable<Boolean>
        get() = _buffer

    private val _maxAcceptableError = RemoteSyncUpdatable<Quantity<Voltage>>()
    public override val maxAcceptableError: UpdatableQuantity<Voltage>
        get() = _maxAcceptableError

    private val _maxVoltage = RemoteSyncUpdatable<Quantity<Voltage>>()
    public override val maxVoltage: UpdatableQuantity<Voltage>
        get() = _maxVoltage

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(Boolean.serializer(), RC.BUFFER) {
                send(source = _buffer.localSetChannel)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    _buffer.update(it)
                }
            }
            bindFS<Quantity<Voltage>>(Quantity.serializer(), RC.MAX_ACCEPTABLE_ERROR) {
                send(source = _maxAcceptableError.localSetChannel)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    _maxAcceptableError.update(it)
                }
            }
            bindFS<Quantity<Voltage>>(Quantity.serializer(), RC.MAX_VOLTAGE) {
                send(source = _maxVoltage.localSetChannel)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    _maxVoltage.update(it)
                }
            }
        }
    }

}