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

package org.tenkiv.kuantify.fs.hardware.channel

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.acquire.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import javax.measure.quantity.*

internal fun CombinedNetworkRouting.combinedAnalogInputRouting(analogInput: AnalogInput) {

    bind<Boolean>(RC.BUFFER) {
        serializeMessage {
            Json.stringify(BooleanSerializer, it)
        } withSerializer {
            receiveMessageOnEither {
                val setting = Json.parse(BooleanSerializer, it)
                analogInput.buffer.set(setting)
            }
        }

        setLocalUpdateChannel(analogInput.buffer.updateBroadcaster.openSubscription()) withUpdateChannel {
            sendFromHost()
            sendFromRemote()
        }
    }

    bind<ComparableQuantity<ElectricPotential>>(RC.MAX_ACCEPTABLE_ERROR) {
        serializeMessage {
            Json.stringify(ComparableQuantitySerializer, it)
        } withSerializer {
            receiveMessageOnEither {
                val setting = Json.parse(ComparableQuantitySerializer, it).asType<ElectricPotential>().toDaqc()
                analogInput.maxAcceptableError.set(setting)
            }
        }

        setLocalUpdateChannel(analogInput.maxAcceptableError.updateBroadcaster.openSubscription()) withUpdateChannel {
            sendFromRemote()
            sendFromHost()
        }
    }

    bind<ComparableQuantity<ElectricPotential>>(RC.MAX_ELECTRIC_POTENTIAL) {
        serializeMessage {
            Json.stringify(ComparableQuantitySerializer, it)
        } withSerializer {
            receiveMessageOnEither {
                val setting = Json.parse(ComparableQuantitySerializer, it).asType<ElectricPotential>().toDaqc()
                analogInput.maxElectricPotential.set(setting)
            }
        }

        setLocalUpdateChannel(analogInput.maxElectricPotential.updateBroadcaster.openSubscription()) withUpdateChannel {
            sendFromRemote()
            sendFromHost()
        }
    }
}

interface LocalAnalogInput : AnalogInput, LocalQuantityInput<ElectricPotential>,
    NetworkBoundCombined {

    override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            combinedAnalogInputRouting(this@LocalAnalogInput)
        }
    }

}

abstract class FSRemoteAnalogInput<D>(override val device: D, uid: String) :
    FSRemoteQuantityInput<ElectricPotential>(device.coroutineContext, uid),
    AnalogInput,
    NetworkBoundCombined where D : AnalogDaqDevice, D : FSRemoteDevice {

    override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            combinedAnalogInputRouting(this@FSRemoteAnalogInput)
        }
    }
}