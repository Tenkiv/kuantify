package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityInput
import javax.measure.quantity.Frequency

class SimpleDigitalFrequencySensor(val digitalInput: DigitalInput) : QuantityInput<Frequency> {

    override val broadcastChannel get() = digitalInput.transitionFrequencyBroadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForTransitionFrequency

    override fun activate() = digitalInput.activateForTransitionFrequency()

    override fun deactivate() = digitalInput.deactivate()

}