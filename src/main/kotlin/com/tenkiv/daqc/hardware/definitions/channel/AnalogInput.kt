package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.QuantityMeasurement
import com.tenkiv.daqc.hardware.definitions.DaqcChannel
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class AnalogInput :
        Input<QuantityMeasurement<ElectricPotential>>,
        DaqcChannel<DaqcQuantity<ElectricPotential>> {

    abstract var buffer: Boolean

    abstract var accuracy: ComparableQuantity<ElectricPotential>

}