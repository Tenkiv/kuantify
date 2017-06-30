package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.hardware.definitions.Channel
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.Dimensionless

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalOutput : Output<BinaryState>,
        Channel<BinaryState> {

    abstract val canPulseWidthModulate: Boolean

    abstract fun pulseWidthModulate(percent: ComparableQuantity<Dimensionless>)

}