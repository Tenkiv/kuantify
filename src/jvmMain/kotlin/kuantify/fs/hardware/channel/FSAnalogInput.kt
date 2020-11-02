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

package kuantify.fs.hardware.channel

import kotlinx.coroutines.channels.*
import kotlinx.serialization.builtins.*
import kuantify.*
import kuantify.fs.gate.acquire.*
import kuantify.fs.hardware.device.*
import kuantify.fs.networking.*
import kuantify.hardware.channel.*
import kuantify.hardware.device.*
import kuantify.lib.physikal.*
import kuantify.networking.communication.*
import kuantify.networking.configuration.*
import kuantify.trackable.*
import physikal.*

public abstract class LocalAnalogInput<out DeviceT>(
    uid: String,
    public final override val device: DeviceT
) : LocalQuantityInput<Voltage>(uid), AnalogInput where DeviceT : LocalDevice, DeviceT : AnalogDaqDevice {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(Boolean.serializer(), RC.BUFFER) {
                send(source = buffer.flow)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    buffer.set(it)
                }
            }
            bindFS<Quantity<Voltage>>(Quantity.serializer(), RC.MAX_ACCEPTABLE_ERROR) {
                send(source = maxAcceptableError.flow)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    maxAcceptableError.set(it)
                }
            }
            bindFS<Quantity<Voltage>>(Quantity.serializer(), RC.MAX_VOLTAGE) {
                send(source = maxVoltage.flow)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    maxVoltage.set(it)
                }
            }
            bindFS<Quantity<Frequency>>(Quantity.serializer(), RC.UPDATE_RATE) {
                send(source = updateRate.flow)
            }
        }
    }

}

public abstract class FSRemoteAnalogInput<out DeviceT>(
    uid: String,
    public final override val device: DeviceT
) : FSRemoteQuantityInput<Voltage>(uid), AnalogInput where DeviceT : AnalogDaqDevice, DeviceT : FSRemoteDevice {
    private val _buffer = RemoteSyncUpdatable<Boolean> {
        modifyConfiguration {
            command {
                setValue(it)
            }
        }
    }
    public override val buffer: Updatable<Boolean>
        get() = _buffer

    private val _maxAcceptableError = RemoteSyncUpdatable<Quantity<Voltage>> {
        modifyConfiguration {
            command {
                setValue(it)
            }
        }
    }
    public override val maxAcceptableError: UpdatableQuantity<Voltage>
        get() = _maxAcceptableError

    private val _maxVoltage = RemoteSyncUpdatable<Quantity<Voltage>> {
        modifyConfiguration {
            command {
                setValue(it)
            }
        }
    }
    public override val maxVoltage: UpdatableQuantity<Voltage>
        get() = _maxVoltage

    private val _updateRate = Updatable<Quantity<Frequency>> {
        modifyConfiguration {
            setValue(it)
        }
    }
    public override val updateRate: TrackableQuantity<Frequency> get() = _updateRate

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(Boolean.serializer(), RC.BUFFER) {
                send(source = _buffer.localSetChannel)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    modifyConfiguration {
                        _buffer.update(it)
                    }
                }
            }
            bindFS<Quantity<Voltage>>(Quantity.serializer(), RC.MAX_ACCEPTABLE_ERROR) {
                send(source = _maxAcceptableError.localSetChannel)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    modifyConfiguration {
                        _maxAcceptableError.update(it)
                    }
                }
            }
            bindFS<Quantity<Voltage>>(Quantity.serializer(), RC.MAX_VOLTAGE) {
                send(source = _maxVoltage.localSetChannel)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    modifyConfiguration {
                        _maxVoltage.update(it)
                    }
                }
            }
            bindFS<Quantity<Frequency>>(Quantity.serializer(), RC.UPDATE_RATE) {
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    _updateRate.set(it)
                }
            }
        }
    }

}