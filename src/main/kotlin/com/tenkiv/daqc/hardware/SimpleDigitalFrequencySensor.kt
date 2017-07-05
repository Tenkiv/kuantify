package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.QuantityMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import javax.measure.quantity.Frequency

class SimpleDigitalFrequencySensor(val digitalInput: DigitalInput) : Input<QuantityMeasurement<Frequency>> {

    override val broadcastChannel get() = digitalInput.transitionFrequencyBroadcastChannel

    override val isActive get() = digitalInput.isActiveForTransitionFrequency

    override fun activate() = digitalInput.activateForTransitionFrequency()

    override fun deactivate() = digitalInput.deactivate()

}