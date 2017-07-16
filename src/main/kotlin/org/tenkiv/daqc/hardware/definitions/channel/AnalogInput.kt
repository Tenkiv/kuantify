package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.hardware.definitions.DaqcChannel
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency

abstract class AnalogInput :
        QuantityInput<ElectricPotential>,
        DaqcChannel {

    abstract var buffer: Boolean

    abstract val sampleRate: ComparableQuantity<Frequency>

    abstract var maxAcceptableError: ComparableQuantity<ElectricPotential>

    abstract var maxElectricPotential: ComparableQuantity<ElectricPotential>

}