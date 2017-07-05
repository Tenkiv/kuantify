package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.Measurement
import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.definitions.DigitalDaqcChannel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

abstract class DigitalInput :
        Input<Measurement>,
        DigitalDaqcChannel {

    val transitionFrequencyBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<Frequency>>>()

    val pwmBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<Dimensionless>>>()

    val currentStateBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<BinaryState>>()

    override val isActive get() = super.isActive

    abstract fun activateForTransitionFrequency()

    abstract fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>)

    open fun activateForCurrentState() = activate()
}