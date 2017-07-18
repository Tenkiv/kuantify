package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.Updatable

typealias StandardQuantityOutput<Q> = Output<DaqcQuantity<Q>, DaqcQuantity<Q>>
typealias StandardBinaryStateOutput = Output<BinaryState, BinaryState>

typealias AnyQuantityOutput<Q> = Output<DaqcQuantity<Q>, out DaqcValue>
typealias AnyBinaryStateOutput = Output<BinaryState, out DaqcValue>
typealias AnyQuantityOnlyOutput<Q> = Output<DaqcQuantity<Q>, out DaqcQuantity<*>>

/**
 * Most abstract representation of an output to control a physical property. It should usually be accessed through one
 * of its type aliases.
 *
 * @param P The "primary" type controlled by this output. If this interface is accessed directly this is the only type
 * that can be set.
 *
 * @param U The type of the updates on what value was just sent out by this output. The reason it may be different from
 * [P] is the implementing class may have options to set more types than just [P], which we may want to receive updates
 * on even though we cannot set the output to said type when accessing the interface directly.
 *
 * There are only two practical settings for this are [DaqcQuantity] of * and [DaqcValue]. If [U] is not set to one of
 * these types implement the [StandardQuantityOutput] or [StandardBinaryStateOutput] type alias.
 */
interface Output<in P : DaqcValue, U : DaqcValue> : Updatable<ValueInstant<U>> {

    val isActive: Boolean

    fun setOutput(setting: P)

    fun deactivate()

}
