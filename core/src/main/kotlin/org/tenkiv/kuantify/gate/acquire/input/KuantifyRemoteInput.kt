package org.tenkiv.kuantify.gate.acquire.input

import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import tec.units.indriya.*
import javax.measure.*
import kotlin.coroutines.*
import kotlin.reflect.*

sealed class KuanitfyRemoteInput<T : DaqcValue>(val device: RemoteKuantifyDevice) : Input<T> {

    override val coroutineContext: CoroutineContext
        get() = device.coroutineContext

    internal val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

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

}

abstract class QuantityKuantifyRemoteInput<Q : Quantity<Q>>(device: RemoteKuantifyDevice) :
    KuanitfyRemoteInput<DaqcQuantity<Q>>(device), QuantityInput<Q> {

    abstract val quantityType: KClass<Q>

    internal fun unsafeUpdate(measurement: ValueInstant<ComparableQuantity<*>>) {
        val (value, instant) = measurement

        _updateBroadcaster.offer(value.asType(quantityType.java).toDaqc() at instant)
    }

}

abstract class BinaryStateKuantifyRemoteInput(device: RemoteKuantifyDevice) : KuanitfyRemoteInput<BinaryState>(device),
    BinaryStateInput
