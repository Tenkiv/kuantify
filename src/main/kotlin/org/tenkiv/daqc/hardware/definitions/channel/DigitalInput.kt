package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.DigitalDaqcChannel
import javax.measure.quantity.Frequency

abstract class DigitalInput : BinaryStateChannel(), BinaryStateInput, DigitalDaqcChannel {

    override val isActive get() = super.isActive

    abstract fun activateForTransitionFrequency()

    abstract fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>)

    open fun activateForCurrentState() = activate()
}