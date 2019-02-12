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

package org.tenkiv.kuantify.hardware.definitions.channel

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import javax.measure.quantity.*

internal fun CombinedRouteConfig.combinedAnalogInputRouting(analogInput: AnalogInput, inputUid: String) {
    val inputRoute = listOf(RC.DAQC_GATE, inputUid)

    route(inputRoute + RC.BUFFER) to handler<Boolean>(isFullyBiDirectional = true) {
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

    route(inputRoute + RC.MAX_ACCEPTABLE_ERROR) to handler<ComparableQuantity<ElectricPotential>>(
        isFullyBiDirectional = true
    ) {
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

    route(inputRoute + RC.MAX_ELECTRIC_POTENTIAL) to handler<ComparableQuantity<ElectricPotential>>(
        isFullyBiDirectional = true
    ) {
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

interface LocalAnalogInput : AnalogInput, LocalQuantityInput<ElectricPotential>, NetworkConfiguredCombined {

    override fun combinedConfig(config: CombinedRouteConfig) {
        config.add {
            combinedAnalogInputRouting(this@LocalAnalogInput, uid)
        }
    }

}

abstract class FSRemoteAnalogInput : FSRemoteQuantityInput<ElectricPotential>(),
    AnalogInput, NetworkConfiguredCombined {

    override fun combinedConfig(config: CombinedRouteConfig) {
        config.add {
            combinedAnalogInputRouting(this@FSRemoteAnalogInput, uid)
        }
    }
}