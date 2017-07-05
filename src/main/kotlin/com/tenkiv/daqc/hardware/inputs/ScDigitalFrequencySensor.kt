package com.tenkiv.daqc.hardware.inputs

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.QuantityMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.lib.openNewCoroutineListener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.at
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Frequency


abstract class ScDigitalFrequencySensor<Q : Quantity<Q>>(val digitalInput: DigitalInput) :
        Input<QuantityMeasurement<Q>> {

    override val broadcastChannel: ConflatedBroadcastChannel<QuantityMeasurement<Q>> = ConflatedBroadcastChannel()

    override val isActive get() = digitalInput.isActiveForTransitionFrequency

    init {
        digitalInput.transitionFrequencyBroadcastChannel.openNewCoroutineListener(CommonPool) {
            processNewMeasurement(convertInput(it.value) at it.instant)
        }
    }

    override fun activate() = digitalInput.activateForTransitionFrequency()

    override fun deactivate() = digitalInput.deactivate()

    protected abstract fun convertInput(frequency: ComparableQuantity<Frequency>): DaqcQuantity<Q>
}