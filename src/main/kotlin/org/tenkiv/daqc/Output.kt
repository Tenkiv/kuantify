package org.tenkiv.daqc

import org.tenkiv.coral.ValueInstant
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity

typealias QuantityOutput<Q> = Output<DaqcQuantity<Q>>
typealias BinaryStateOutput = Output<BinaryState>

fun <Q : Quantity<Q>> QuantityOutput<Q>.setOutput(setting: ComparableQuantity<Q>) = setOutput(setting.toDaqc())

interface Output<T : DaqcValue> : Updatable<ValueInstant<T>> {

    val isActive: Boolean

    /**
     * @throws Throwable if something prevents this output from being set.
     */
    fun setOutput(setting: T)

    fun deactivate()

}
