package org.tenkiv.kuantify.gate.control.output

import kotlinx.coroutines.channels.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.*
import kotlin.coroutines.*
import kotlin.reflect.*

sealed class KuanitfyRemoteOutput<T : DaqcValue>(val device: RemoteKuantifyDevice) : Output<T>, NetworkConfiguredSide {

    abstract val uid: String

    override val coroutineContext: CoroutineContext
        get() = device.coroutineContext

    internal val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

    override fun setOutput(setting: T, panicOnFailure: Boolean): SettingResult.Success {
        _updateBroadcaster.offer(setting.now())
        return SettingResult.Success
    }

    internal val _isTransceiving = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    internal val stopTransceivingChannel = Channel<Unit>(Channel.CONFLATED)
    override fun stopTransceiving() {
        stopTransceivingChannel.offer(Unit)
    }

    override fun sideConfig(config: SideRouteConfig) {
        val outputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(outputRoute + RC.IS_TRANSCEIVING) to handler<Boolean>(isFullyBiDirectional = false) {
                receiveMessage(NullResolutionStrategy.PANIC) {
                    val value = Json.parse(BooleanSerializer, it)
                    _isTransceiving.value = value
                }
            }

            route(outputRoute + RC.STOP_TRANSCEIVING) to handler<Unit>(isFullyBiDirectional = false) {
                setLocalUpdateChannel(stopTransceivingChannel) withUpdateChannel {
                    send()
                }
            }
        }
    }
}

abstract class QuantityKuantifyRemoteOutput<Q : Quantity<Q>>(device: RemoteKuantifyDevice) :
    KuanitfyRemoteOutput<DaqcQuantity<Q>>(device), QuantityOutput<Q> {

    abstract val quantityType: KClass<Q>

    override fun sideConfig(config: SideRouteConfig) {
        super.sideConfig(config)
        val outputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(outputRoute + RC.VALUE) to handler<QuantityMeasurement<Q>>(isFullyBiDirectional = true) {
                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val (value, instant) = Json.parse(ValueInstantSerializer(ComparableQuantitySerializer), it)
                    val setting = value.asType<Q>(quantityType.java).toDaqc()

                    _updateBroadcaster.send(setting at instant)
                }
            }
        }
    }
}

abstract class BinaryStateKuantifyRemoteOutput(device: RemoteKuantifyDevice) :
    KuanitfyRemoteOutput<BinaryState>(device), BinaryStateOutput {

    override fun sideConfig(config: SideRouteConfig) {
        super.sideConfig(config)
        val outputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(outputRoute + RC.VALUE) to handler<BinaryStateMeasurement>(isFullyBiDirectional = true) {
                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val settingInstant = Json.parse(ValueInstantSerializer(BinaryState.serializer()), it)

                    _updateBroadcaster.send(settingInstant)
                }
            }
        }
    }
}