package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.Updatable

typealias QuantityOutput<Q> = Output<DaqcQuantity<Q>>
typealias BinaryStateOutput = Output<BinaryState>

interface Output<T : DaqcValue> : Updatable<ValueInstant<T>> {

    val isActive: Boolean

    fun setOutput(setting: T)

    fun deactivate()

}
