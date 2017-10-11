package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.QuantityInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.toDaqc
import org.tenkiv.physikal.core.hertz
import javax.measure.quantity.Frequency

class SimpleDigitalFrequencySensor internal constructor(val digitalInput: DigitalInput) : QuantityInput<Frequency> {

    @Volatile
    var avgFrequency: DaqcQuantity<Frequency> = 1.hertz.toDaqc()

    override val broadcastChannel get() = digitalInput.transitionFrequencyBroadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForTransitionFrequency

    override fun activate() = digitalInput.activateForTransitionFrequency(avgFrequency)

    override fun deactivate() = digitalInput.deactivate()

}