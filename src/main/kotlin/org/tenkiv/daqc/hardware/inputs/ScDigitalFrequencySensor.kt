package org.tenkiv.daqc.hardware.inputs

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.at
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityInput
import org.tenkiv.daqc.lib.openNewCoroutineListener
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Frequency


abstract class ScDigitalFrequencySensor<Q : Quantity<Q>>(val digitalInput: DigitalInput) :
        QuantityInput<Q> {

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()

    final override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    override val isActive get() = digitalInput.isActiveForTransitionFrequency

    init {
        digitalInput.transitionFrequencyBroadcastChannel.openNewCoroutineListener(CommonPool) {
            _broadcastChannel.send(convertInput(it.value) at it.instant)
        }
    }

    override fun activate() = digitalInput.activateForTransitionFrequency()

    override fun deactivate() = digitalInput.deactivate()

    protected abstract fun convertInput(frequency: ComparableQuantity<Frequency>): DaqcQuantity<Q>
}