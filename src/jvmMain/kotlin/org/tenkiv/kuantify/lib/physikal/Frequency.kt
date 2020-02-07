/*
 * Copyright 2020 Tenkiv, Inc.
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

package org.tenkiv.kuantify.lib.physikal

import kotlinx.serialization.*
import physikal.*
import physikal.types.*
import kotlin.reflect.*

public interface Frequency : Quantity<Frequency>

@Serializable
@SerialName(Hertz.SYMBOL)
internal class HertzQuantity(override val inOwnUnit: Double) : Quantity<Frequency> {
    override val unit: PhysicalUnit<Frequency> get() = Hertz

    override fun convertToDefaultUnit(): Quantity<Frequency> = this

    override fun toString(): String = "$inOwnUnit ${unit.symbol}"
}

public val Double.hertz: Quantity<Frequency> get() = HertzQuantity(this)

@Serializable
@SerialName(Hertz.SYMBOL)
public object Hertz : PhysicalUnit<Frequency> {
    public const val SYMBOL: String = "Hz"

    public override val default: PhysicalUnit<Frequency> get() = this
    public override val quantityType: KClass<Frequency> get() = Frequency::class
    public override val symbol: String get() = SYMBOL

    public override fun quantityOf(amount: Double): Quantity<Frequency> = amount.hertz

    public override fun quantityOfInDefaultUnit(amount: Double): Quantity<Frequency> = amount.hertz

    public override fun toString(): String = Second.SYMBOL
}

public fun Quantity<Frequency>.toPeriod(): Quantity<Time> = this.transform(Hertz) {
    (1 / it).secondsQuantity
}