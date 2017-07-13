package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.QuantityMeasurement
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.Input
import javax.measure.quantity.Frequency

class SimpleDigitalFrequencySensor(val digitalInput: DigitalInput) : Input<QuantityMeasurement<Frequency>> {

    override val broadcastChannel get() = digitalInput.transitionFrequencyBroadcastChannel

    override val isActive get() = digitalInput.isActiveForTransitionFrequency

    override fun activate() = digitalInput.activateForTransitionFrequency()

    override fun deactivate() = digitalInput.deactivate()

}