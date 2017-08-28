package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.DigitalDaqcChannel
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


abstract class DigitalOutput : BinaryStateChannel(), BinaryStateOutput,
        DigitalDaqcChannel {

    override val isActive get() = super.isActive

    abstract fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>)

    abstract fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>)

    override fun deactivate() = setOutput(BinaryState.Off)

}