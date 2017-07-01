package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import org.tenkiv.coral.ValueInstant
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.Frequency

abstract class DigitalInput :
        Input<ValueInstant<DaqcValue>>,
        Channel<DaqcValue> {

    abstract fun activateForTransitionFrequency()

    abstract fun activateForPwm(avgFrequency: ComparableQuantity<Frequency>)

    open fun activateForCurrentState() = activate()
}