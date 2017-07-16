package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.DigitalDaqcChannel
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


abstract class DigitalOutput : MultiOutput<BinaryState>,
        DigitalDaqcChannel {

    abstract val pwmIsSimulated: Boolean

    abstract val transitionFrequencyIsSimulated: Boolean

    override val isActive get() = super.isActive

    override fun deactivate() = setOutput(BinaryState.Off)

    abstract fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>)

    abstract fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>)

}