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