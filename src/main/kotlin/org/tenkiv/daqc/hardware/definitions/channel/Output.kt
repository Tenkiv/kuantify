package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.Updatable

typealias QuantityOutput<Q> = StandardOutput<DaqcQuantity<Q>>
typealias BinaryStateOutput = StandardOutput<BinaryState>

/**
 * Base interface for a control output. Should never be used directly, in most cases [StandardOutput] should be used.
 *
 * @param T The type this output controls.
 */
interface OutputCore<in T : DaqcValue> {

    val isActive: Boolean

    fun setOutput(setting: T)

    fun deactivate()

}

/**
 *
 * @param T The type this output controls.
 */
interface StandardOutput<T : DaqcValue> : OutputCore<T>, Updatable<ValueInstant<T>>

/**
 * An [Updatable] output whose implementing class can output multiple different types of DaqcValues. This interface
 * should only be used when you're sure the abstraction cannot be achieved with one or multiple [StandardOutput]s.
 *
 * @param P The "primary" type outputted by this Output. This is the only type that can be set if this is seen as an
 * [OutputCore] or a [MultiOutput]. To set any potential output type, access lower abstraction directly.
 */
interface MultiOutput<in P : DaqcValue> : OutputCore<P>, Updatable<ValueInstant<DaqcValue>>