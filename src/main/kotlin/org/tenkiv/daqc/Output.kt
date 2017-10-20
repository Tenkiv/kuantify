package org.tenkiv.daqc

import org.tenkiv.coral.ValueInstant

typealias QuantityOutput<Q> = Output<DaqcQuantity<Q>>
typealias BinaryStateOutput = Output<BinaryState>

interface Output<T : DaqcValue> : Updatable<ValueInstant<T>> {

    val isActive: Boolean

    /**
     * @throws Throwable if something prevents this output from being set.
     */
    fun setOutput(setting: T)

    fun deactivate()

}
