package org.tenkiv.kuantify.gate.acquire.input

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.*

interface LocalInput<T : DaqcValue> : Input<T>, NetworkConfiguredSide {
    val uid: String

    override fun sideConfig(config: SideRouteConfig) {
        val inputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(inputRoute + RC.IS_TRANSCEIVING) to handler<Boolean>(isFullyBiDirectional = false) {
                serializeMessage {
                    Json.stringify(BooleanSerializer, it)
                }

                setLocalUpdateChannel(isTransceiving.updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }

            route(inputRoute + RC.START_SAMPLING) to handler<Ping>(isFullyBiDirectional = false) {
                receive {
                    startSampling()
                }
            }

            route(inputRoute + RC.STOP_TRANSCEIVING) to handler<Ping>(isFullyBiDirectional = false) {
                receive {
                    stopTransceiving()
                }
            }
        }
    }
}

interface LocalQuantityInput<Q : Quantity<Q>> : LocalInput<DaqcQuantity<Q>>, QuantityInput<Q> {


    override fun sideConfig(config: SideRouteConfig) {
        super.sideConfig(config)
        val inputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(inputRoute + RC.VALUE) to handler<QuantityMeasurement<Q>>(isFullyBiDirectional = false) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(ComparableQuantitySerializer), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}

interface LocalBinaryStateInput : LocalInput<BinaryState>, BinaryStateInput {

    override fun sideConfig(config: SideRouteConfig) {
        super.sideConfig(config)
        val inputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(inputRoute + RC.VALUE) to handler<BinaryStateMeasurement>(isFullyBiDirectional = false) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(BinaryState.serializer()), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}