package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.DaqcChannel
import com.tenkiv.daqc.hardware.definitions.Updatable
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


abstract class DigitalOutput : Output<BinaryState>,
        DaqcChannel<BinaryState>,
        Updatable<DaqcValue> {

    abstract val pwmIsSimulated: Boolean

    abstract val transitionFrequencyIsSimulated: Boolean

    abstract fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>)

    abstract fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>)

}