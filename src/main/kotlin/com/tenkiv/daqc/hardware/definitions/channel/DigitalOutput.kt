package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.hardware.definitions.Channel
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.Dimensionless


abstract class DigitalOutput : Output<BinaryState>,
        Channel<BinaryState> {

    abstract val canPulseWidthModulate: Boolean

    abstract fun pulseWidthModulate(percent: ComparableQuantity<Dimensionless>)

}