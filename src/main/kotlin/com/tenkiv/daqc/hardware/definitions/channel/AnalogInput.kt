package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.QuantityMeasurement
import com.tenkiv.daqc.hardware.definitions.DaqcChannel
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential

abstract class AnalogInput :
        Input<QuantityMeasurement<ElectricPotential>>,
        DaqcChannel<DaqcQuantity<ElectricPotential>> {

    abstract var buffer: Boolean

    abstract var maxAcceptableError: ComparableQuantity<ElectricPotential>

    abstract var maxElectricPotential: ComparableQuantity<ElectricPotential>

}