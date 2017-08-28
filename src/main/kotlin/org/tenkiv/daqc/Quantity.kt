package org.tenkiv.daqc


import tec.uom.se.ComparableQuantity
import javax.measure.Quantity


fun <Q : Quantity<Q>> ComparableQuantity<Q>.toDaqc() = DaqcQuantity(this)

/**
 * Function to add two [Quantity]s.
 *
 * @return [Quantity] with added value.
 */
//operator fun <Q : Quantity<Q>> DaqcQuantity<Q>.plus(augend: DaqcQuantity<Q>): DaqcQuantity<Q> = DaqcQuantity(add(augend))

/**
 * Function to subtract two [Quantity]s.
 *
 * @return [Quantity] with subtracted value.
 */
//operator fun <Q : Quantity<Q>> DaqcQuantity<Q>.minus(subtrahend: DaqcQuantity<Q>): DaqcQuantity<Q> = DaqcQuantity(subtract(subtrahend))

/**
 * Function to multiply two [Quantity]s.
 *
 * @return [Quantity] with multiplied value.
 */
//operator fun <Q : Quantity<Q>> DaqcQuantity<Q>.times(multiplier: Number): DaqcQuantity<Q> = DaqcQuantity(multiply(multiplier))

/**
 * Function to divide two [Quantity]s.
 *
 * @return [Quantity] with divided value.
 */
//operator fun <Q : Quantity<Q>> DaqcQuantity<Q>.div(divisor: Number): DaqcQuantity<Q> = DaqcQuantity(divide(divisor))