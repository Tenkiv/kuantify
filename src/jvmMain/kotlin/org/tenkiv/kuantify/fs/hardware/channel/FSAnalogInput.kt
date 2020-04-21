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

import kotlinx.serialization.builtins.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.gate.acquire.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.lib.physikal.*
import physikal.*

internal fun CombinedNetworkRouting.combinedAnalogInputRouting(analogInput: AnalogInput<*>) {
    bind<Boolean>(RC.BUFFER, recursiveSynchronizer = true) {
        serializeMessage {
            Serialization.json.stringify(Boolean.serializer(), it)
        } withSerializer {
            receiveMessageOnEither {
                val setting = Serialization.json.parse(Boolean.serializer(), it)
                analogInput.buffer.set(setting)
            }
        }

        setLocalUpdateChannel(analogInput.buffer.updateBroadcaster.openSubscription()) withUpdateChannel {
            sendFromHost()
            sendFromRemote()
        }
    }

    bind<Quantity<Voltage>>(RC.MAX_ACCEPTABLE_ERROR, recursiveSynchronizer = true) {
        serializeMessage {
            Serialization.json.stringify(Quantity.serializer(), it)
        } withSerializer {
            receiveMessageOnEither {
                val setting = Serialization.json.parse(Quantity.serializer<Voltage>(), it)
                analogInput.maxAcceptableError.set(setting)
            }
        }

        setLocalUpdateChannel(analogInput.maxAcceptableError.updateBroadcaster.openSubscription()) withUpdateChannel {
            sendFromRemote()
            sendFromHost()
        }
    }

    bind<Quantity<Voltage>>(RC.MAX_ELECTRIC_POTENTIAL, recursiveSynchronizer = true) {
        serializeMessage {
            Serialization.json.stringify(Quantity.serializer(), it)
        } withSerializer {
            receiveMessageOnEither {
                val setting = Serialization.json.parse(Quantity.serializer<Voltage>(), it)
                analogInput.maxVoltage.set(setting)
            }
        }

        setLocalUpdateChannel(analogInput.maxVoltage.updateBroadcaster.openSubscription()) withUpdateChannel {
            sendFromRemote()
            sendFromHost()
        }
    }

}

public abstract class LocalAnalogInput<DeviceT>(uid: String) : LocalQuantityInput<Voltage>(uid), AnalogInput
        where DeviceT : LocalDevice, DeviceT : AnalogDaqDevice {
    public abstract override val device: DeviceT

}

public abstract class FSRemoteAnalogInput<DeviceT>(uid: String) : FSRemoteQuantityInput<Voltage>(uid), AnalogInput
        where DeviceT : AnalogDaqDevice, DeviceT : FSRemoteDevice {
    public abstract override val device: DeviceT

}