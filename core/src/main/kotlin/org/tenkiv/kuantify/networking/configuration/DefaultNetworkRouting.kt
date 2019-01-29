package org.tenkiv.kuantify.networking.configuration

import kotlinx.serialization.json.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*

fun NetworkCommunicatorBuilder.addBuiltinRouting() {

    device.daqcGateMap.forEach { id, gate ->
        when (gate) {
            is Input<*> -> route(RC.DAQC_GATE, id, RC.VALUE) to handler(
                gate.updateBroadcaster.openSubscription()
            ) {
                serializeUpdates {
                    when (val value = it.value) {
                        is BinaryState -> Json.stringify(BinaryState.serializer(), value)
                        is DaqcQuantity<*> -> Json.stringify(ComparableQuantitySerializer, value)
                    }
                } withSerializer {

                }

                sendFromHost()
                receivePingsOnRemote {

                }
            }
        }
    }

}