package org.tenkiv.kuantify.networking.configuration

import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*

private val logger = KotlinLogging.logger {}

fun CombinedRouteConfig.addStandardRouting() {

    device.daqcGateMap.forEach { id, gate ->
        when (gate) {
            is Input<*> -> addInputRouting(id, gate)
            else -> logger.debug {
                """Gate $id on device $device is not of a type supported by standard routing,
                    | see if it can be modified to fit a supported type, or ensure custom routing
                    | is provided for it.""".trimMargin()
            }
        }
    }

}

@Suppress("UNCHECKED_CAST")
private fun CombinedRouteConfig.addInputRouting(id: String, input: Input<*>) {
    val thisInputRc = route(RC.DAQC_GATE, id)

    route(thisInputRc + RC.VALUE) to handler(input.updateBroadcaster.openSubscription(), isFullyBiDirectional = false) {
        sendFromHost()

        serializeMessage {
            when (it.value) {
                is BinaryState -> {
                    val measurement = it as ValueInstant<BinaryState>
                    Json.stringify(ValueInstantSerializer(BinaryState.serializer()), measurement)
                }
                is DaqcQuantity<*> -> {
                    val measurement = it as ValueInstant<DaqcQuantity<*>>
                    Json.stringify(ValueInstantSerializer(ComparableQuantitySerializer), measurement)
                }
            }
        } withSerializer {
            when (input) {
                is QuantityKuantifyRemoteInput<*> -> receiveMessageOnRemote {

                }
                is BinaryStateKuantifyRemoteInput -> receiveMessageOnRemote {

                }
            }
        }
    }
}