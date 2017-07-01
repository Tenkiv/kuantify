package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.hardware.definitions.Channel
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


abstract class DigitalOutput : Output<BinaryState>,
        Channel<BinaryState> {

    abstract val pwmIsSimulated: Boolean

    abstract val transitionFrequencyIsSimulated: Boolean

    abstract fun pulseWidthModulate(percent: ComparableQuantity<Dimensionless>)

    abstract fun setTransitionFrequency(freq: ComparableQuantity<Frequency>)

}