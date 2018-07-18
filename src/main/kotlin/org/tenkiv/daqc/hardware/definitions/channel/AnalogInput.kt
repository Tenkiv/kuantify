package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.QuantityInput
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential

abstract class AnalogInput : QuantityInput<ElectricPotential>, DaqcChannel {

    /**
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract var buffer: Boolean

    /**
     * Implementing backing field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract var maxAcceptableError: ComparableQuantity<ElectricPotential>

    /**
     * Implementing backing field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract var maxElectricPotential: ComparableQuantity<ElectricPotential>

}