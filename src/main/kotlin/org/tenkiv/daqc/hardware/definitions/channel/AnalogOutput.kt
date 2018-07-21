package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.QuantityOutput
import javax.measure.quantity.ElectricPotential

abstract class AnalogOutput : QuantityOutput<ElectricPotential>, DaqcChannel {

    override val quantityType get() = ElectricPotential::class

}