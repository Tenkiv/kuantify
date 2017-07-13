package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.DigitalDaqcChannel
import org.tenkiv.daqc.hardware.definitions.Updatable
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


abstract class DigitalOutput : Output<BinaryState>,
        DigitalDaqcChannel,
        Updatable<DaqcValue> {

    abstract val pwmIsSimulated: Boolean

    abstract val transitionFrequencyIsSimulated: Boolean

    override val isActive get() = super.isActive

    override fun deactivate() = setOutput(BinaryState.Off)

    abstract fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>)

    abstract fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>)

}