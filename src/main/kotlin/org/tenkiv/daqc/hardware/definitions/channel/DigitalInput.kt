package org.tenkiv.daqc.hardware.definitions.channel

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.DigitalDaqcChannel
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

abstract class DigitalInput :
        Input<DaqcValue>,
        DigitalDaqcChannel {

    val transitionFrequencyBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<Frequency>>>()

    val pwmBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<Dimensionless>>>()

    val currentStateBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<BinaryState>>()

    override val isActive get() = super.isActive

    abstract fun activateForTransitionFrequency()

    abstract fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>)

    open fun activateForCurrentState() = activate()
}