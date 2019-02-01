package org.tenkiv.kuantify.gate.acquire.input

import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import tec.units.indriya.*
import javax.measure.*
import javax.measure.quantity.*
import kotlin.coroutines.*
import kotlin.reflect.*

interface KuantifyRemoteQuantityInput<Q : Quantity<Q>> : QuantityInput<Q> {

    val quantityType: KClass<Q>

    fun unsafeQuantityToDaqc(quantity: ComparableQuantity<*>): DaqcQuantity<Q> {
        return quantity.asType(quantityType.java).toDaqc()
    }

}

abstract class KuanitfyRemoteInput<T : DaqcValue>(val device: RemoteKuantifyDevice) : Input<T> {

    override val coroutineContext: CoroutineContext
        get() = device.coroutineContext

    protected val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
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

/**
 * Configured-Update-Rate-KuantifyRemoteInput.
 * [KuanitfyRemoteInput] where the update is [UpdateRate.Configured] and handled automatically with standard routing.
 */
open class CurKuantifyRemoteInput<T : DaqcValue>(device: RemoteKuantifyDevice) : KuanitfyRemoteInput<T>(device) {

    internal val _updateRate: UpdatableQuantity<Frequency> = Updatable()
    override val updateRate: UpdateRate.Configured = UpdateRate.Configured(_updateRate)

}
