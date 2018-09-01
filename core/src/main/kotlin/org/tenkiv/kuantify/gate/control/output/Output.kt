/*
 * Copyright 2018 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.kuantify.gate.control.output

import org.tenkiv.kuantify.data.BinaryState
import org.tenkiv.kuantify.data.DaqcQuantity
import org.tenkiv.kuantify.data.DaqcValue
import org.tenkiv.kuantify.data.toDaqc
import org.tenkiv.kuantify.gate.IOStrand
import org.tenkiv.kuantify.gate.RangedIOStrand
import org.tenkiv.kuantify.gate.control.attempt.RangeLimitedAttempt
import org.tenkiv.kuantify.gate.control.attempt.Viability
import org.tenkiv.physikal.core.*
import tec.units.indriya.ComparableQuantity
import tec.units.indriya.unit.Units
import javax.measure.Quantity
import kotlin.reflect.KClass

/**
 * Interface defining classes which act as outputs and send controls or signals to devices.
 *
 * @param T The type of signal given by this output.
 */
interface Output<T : DaqcValue> : IOStrand<T> {

    fun setOutput(setting: T): Viability

}

/**
 * An [Output] which sends signals in the form of a [DaqcQuantity].
 *
 * @param Q The type of signal sent by this Output.
 */
interface QuantityOutput<Q : Quantity<Q>> : Output<DaqcQuantity<Q>> {

    /**
     * The [KClass] of [Quantity] which this [QuantityOutput] sends.
     */
    val quantityType: KClass<Q>

    //TODO: check to see if getUnit() already returns the systemUnit, making .systemUnit pointless
    val systemUnit: PhysicalUnit<Q> get() = Units.getInstance().getUnit(quantityType.java).systemUnit

    /**
     * Sets the signal of this output to the correct value as a [DaqcQuantity].
     *
     * @param setting The signal to set as the output.
     */
    fun setOutput(setting: ComparableQuantity<Q>) = setOutput(setting.toDaqc())

    /**
     * @throws ClassCastException
     *           if the dimension of this unit is different from the specified quantity dimension.
     * @throws UnsupportedOperationException
     *           if the specified quantity class does not have a SI unit for the quantity.
     */
    fun dynSetOutput(setting: ComparableQuantity<*>) = setOutput(setting.asType(quantityType.java))

}

/**
 * An [Output] whose type extends both [DaqcValue] and [Comparable] so it can be used in the default learning module.
 */
interface RangedOutput<T> : Output<T>, RangedIOStrand<T> where T : DaqcValue, T : Comparable<T>

/**
 * A [RangedOutput] which uses the [BinaryState] type.
 */
interface BinaryStateOutput : RangedOutput<BinaryState> {

    override val valueRange get() = BinaryState.range

}

// Kotlin compiler is getting confused about generics star projections if RangedOutput (or a typealias) is used directly
// TODO: look into changing this to a typealias if generics compiler issue is fixed.
interface RangedQuantityOutput<Q : Quantity<Q>> : RangedOutput<DaqcQuantity<Q>>, QuantityOutput<Q>

class RangedQuantityOutputBox<Q : Quantity<Q>>(
    private val output: QuantityOutput<Q>,
    override val valueRange: ClosedRange<DaqcQuantity<Q>>
) : RangedQuantityOutput<Q>, QuantityOutput<Q> by output {
    override fun setOutput(setting: DaqcQuantity<Q>): Viability {
        val inRange = setting in valueRange
        return if (!inRange) {
            RangeLimitedAttempt.OUT_OF_RANGE
        } else {
            output.setOutput(setting)
        }
    }
}

/**
 * Converts a [QuantityOutput] to a [RangedQuantityOutputBox] so it can be used in the default learning module.
 *
 * @param valueRange The range of acceptable output values.
 */
fun <Q : Quantity<Q>> QuantityOutput<Q>.toNewRangedOutput(valueRange: ClosedRange<DaqcQuantity<Q>>) =
    RangedQuantityOutputBox(this, valueRange)