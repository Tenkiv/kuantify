package org.tenkiv.kuantify.gate.control.output

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.*
import kotlin.coroutines.*
import kotlin.reflect.*

sealed class LocalOutput<T : DaqcValue>(val device: LocalDevice) : Output<T>, NetworkConfiguredSide {
    abstract val uid: String

    override val coroutineContext: CoroutineContext
        get() = device.coroutineContext

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

abstract class LocalQuantityOutput<Q : Quantity<Q>>(device: LocalDevice) :
    LocalOutput<DaqcQuantity<Q>>(device), QuantityOutput<Q> {

    abstract val quantityType: KClass<Q>

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

abstract class LocalBinaryStateOutput(device: LocalDevice) : LocalOutput<BinaryState>(device),
    BinaryStateOutput {


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