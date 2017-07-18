package org.tenkiv.daqc.hardware.definitions.channel

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.BinaryStateMeasurement
import org.tenkiv.QuantityMeasurement
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.DigitalDaqcChannel
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

abstract class DigitalInput : Input<DaqcValue>, DigitalDaqcChannel {

    val transitionFrequencyBroadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Frequency>>()

    val pwmBroadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Dimensionless>>()

    val currentStateBroadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    override val isActive get() = super.isActive

    abstract fun activateForTransitionFrequency()

    abstract fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>)

    open fun activateForCurrentState() = activate()
}