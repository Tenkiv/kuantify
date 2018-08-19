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

package org.tenkiv.daqc

import org.tenkiv.physikal.core.PhysicalUnit
import org.tenkiv.physikal.core.invoke
import tec.units.indriya.ComparableQuantity
import tec.units.indriya.unit.Units
import javax.measure.Quantity
import kotlin.reflect.KClass

interface Output<T : DaqcValue> : IOChannel<T> {

    //TODO: Maybe change this to return a throwable or something else instead of throwing and exception
    /**
     * @throws Throwable if something prevents this output from being set.
     */
    fun setOutput(setting: T)

}

interface QuantityOutput<Q : Quantity<Q>> : Output<DaqcQuantity<Q>> {

    val quantityType: KClass<Q>

    //TODO: check to see if getUnit() already returns the systemUnit, making .systemUnit pointless
    val systemUnit: PhysicalUnit<Q> get() = Units.getInstance().getUnit(quantityType.java).systemUnit

    fun setOutput(setting: ComparableQuantity<Q>) = setOutput(setting.toDaqc())

    fun setOutputInSystemUnit(setting: Double) = setOutput(setting(systemUnit))

}

interface RangedOutput<T> : Output<T>, RangedIOChannel<T> where T : DaqcValue, T : Comparable<T>

interface BinaryStateOutput : RangedOutput<BinaryState> {

    override val valueRange get() = BinaryState.range

}

// Kotlin compiler is getting confused about generics star projections if RangedOutput (or a typealias) is used directly
// TODO: look into changing this to a typealias if generics compiler issue is fixed.
interface RangedQuantityOutput<Q : Quantity<Q>> : RangedOutput<DaqcQuantity<Q>>, QuantityOutput<Q>

class RangedQuantityOutputBox<Q : Quantity<Q>>(
    output: QuantityOutput<Q>,
    override val valueRange: ClosedRange<DaqcQuantity<Q>>
) : RangedQuantityOutput<Q>, QuantityOutput<Q> by output

fun <Q : Quantity<Q>> QuantityOutput<Q>.toNewRangedOutput(valueRange: ClosedRange<DaqcQuantity<Q>>) =
    RangedQuantityOutputBox(this, valueRange)