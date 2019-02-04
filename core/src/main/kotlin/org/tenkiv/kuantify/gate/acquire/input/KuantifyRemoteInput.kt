package org.tenkiv.kuantify.gate.acquire.input

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
import tec.units.indriya.*
import javax.measure.*
import kotlin.coroutines.*
import kotlin.reflect.*

sealed class KuanitfyRemoteInput<T : DaqcValue>(val device: RemoteKuantifyDevice) : Input<T>,
    NetworkConfiguredRemote {

    abstract val uid: String

    override val coroutineContext: CoroutineContext
        get() = device.coroutineContext

    internal val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

    //TODO
    internal val _failureBroadcaster = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    override val failureBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcaster

    internal val _isTransceiving = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    internal val startSamplingChannel = Channel<Unit>(Channel.CONFLATED)
    override fun startSampling() {
        startSamplingChannel.offer(Unit)
    }

    internal val stopTransceivingChannel = Channel<Unit>(Channel.CONFLATED)
    override fun stopTransceiving() {
        stopTransceivingChannel.offer(Unit)
    }

    override fun remoteConfig(config: SideRouteConfig) {
        val inputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(inputRoute + RC.IS_TRANSCEIVING) to handler<Boolean>(isFullyBiDirectional = false) {
                receiveMessage(NullResolutionStrategy.PANIC) {
                    val value = Json.parse(BooleanSerializer, it)
                    _isTransceiving.value = value
                }
            }

            route(inputRoute + RC.START_SAMPLING) to handler<Unit>(isFullyBiDirectional = false) {
                setLocalUpdateChannel(startSamplingChannel) withUpdateChannel {
                    send()
                }
            }

            route(inputRoute + RC.STOP_TRANSCEIVING) to handler<Unit>(isFullyBiDirectional = false) {
                setLocalUpdateChannel(stopTransceivingChannel) withUpdateChannel {
                    send()
                }
            }
        }
    }
}

abstract class QuantityKuantifyRemoteInput<Q : Quantity<Q>>(device: RemoteKuantifyDevice) :
    KuanitfyRemoteInput<DaqcQuantity<Q>>(device), QuantityInput<Q> {

    abstract val quantityType: KClass<Q>

    private fun unsafeUpdate(measurement: ValueInstant<ComparableQuantity<*>>) {
        val (value, instant) = measurement

        _updateBroadcaster.offer(value.asType(quantityType.java).toDaqc() at instant)
    }

    override fun remoteConfig(config: SideRouteConfig) {
        super.remoteConfig(config)
        val inputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(inputRoute + RC.VALUE) to handler<DaqcQuantity<Q>>(isFullyBiDirectional = false) {
                receiveMessage(NullResolutionStrategy.PANIC) {
                    val measurement = Json.parse(ValueInstantSerializer(ComparableQuantitySerializer), it)
                    unsafeUpdate(measurement)
                }
            }
        }

    }

}

abstract class BinaryStateKuantifyRemoteInput(device: RemoteKuantifyDevice) : KuanitfyRemoteInput<BinaryState>(device),
    BinaryStateInput
