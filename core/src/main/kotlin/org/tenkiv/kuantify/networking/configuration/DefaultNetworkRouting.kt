package org.tenkiv.kuantify.networking.configuration

import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*

fun NetworkConfig<*>.addStandardRouting() {

    device.daqcGateMap.forEach { id, gate ->
        when (gate) {
            is Input<*> -> addInputRouting(id, gate)
        }
    }

}

@Suppress("UNCHECKED_CAST")
private fun NetworkConfig<*>.addInputRouting(id: String, input: Input<*>) {
    val thisInputRc = route(RC.DAQC_GATE, id)

    route(thisInputRc + RC.VALUE) to handler(input.updateBroadcaster.openSubscription()) {
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

            receiveMessageOnRemote {

            }

        }

    }
}