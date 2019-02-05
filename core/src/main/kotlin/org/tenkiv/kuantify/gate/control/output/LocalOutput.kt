package org.tenkiv.kuantify.gate.control.output

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.*
import kotlin.reflect.*

interface LocalOutput<T : DaqcValue> : Output<T>, NetworkConfiguredSide {
    val uid: String

    override fun sideConfig(config: SideRouteConfig) {
        val outputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(outputRoute + RC.IS_TRANSCEIVING) to handler<Boolean>(isFullyBiDirectional = false) {
                serializeMessage {
                    Json.stringify(BooleanSerializer, it)
                }

                setLocalUpdateChannel(isTransceiving.updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }

            route(outputRoute + RC.STOP_TRANSCEIVING) to handler<Unit?>(isFullyBiDirectional = false) {
                receive {
                    stopTransceiving()
                }
            }
        }
    }
}

interface LocalQuantityOutput<Q : Quantity<Q>> : LocalOutput<DaqcQuantity<Q>>, QuantityOutput<Q> {

    val quantityType: KClass<Q>

    override fun sideConfig(config: SideRouteConfig) {
        super.sideConfig(config)
        val outputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(outputRoute + RC.VALUE) to handler<QuantityMeasurement<Q>>(isFullyBiDirectional = true) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(ComparableQuantitySerializer), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val value = Json.parse(ValueInstantSerializer(ComparableQuantitySerializer), it).value
                    val setting = value.asType<Q>(quantityType.java).toDaqc()

                    setOutput(setting)
                }
            }
        }
    }
}

interface LocalBinaryStateOutput : LocalOutput<BinaryState>, BinaryStateOutput {


    override fun sideConfig(config: SideRouteConfig) {
        super.sideConfig(config)
        val outputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(outputRoute + RC.VALUE) to handler<BinaryStateMeasurement>(isFullyBiDirectional = true) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(BinaryState.serializer()), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val setting = Json.parse(ValueInstantSerializer(BinaryState.serializer()), it).value

                    setOutput(setting)
                }
            }
        }
    }
}