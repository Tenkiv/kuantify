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

import physikal.*
import kotlin.reflect.*

public interface Voltage : Quantity<Voltage>

// --- Base --- //

internal class Volts(override val inOwnUnit: Double) : Quantity<Voltage> {
    override val unit: PhysicalUnit<Voltage> get() = Volt

    override fun convertToDefaultUnit(): Quantity<Voltage> = this
}

public val Double.volts: Quantity<Voltage> get() = Volts(this)

public object Volt : PhysicalUnit<Voltage> {
    public const val SYMBOL: String = "V"

    override val default: PhysicalUnit<Voltage> get() = this
    override val quantityType: KClass<Voltage> get() = Voltage::class
    override val symbol: String get() = SYMBOL

    override fun quantityOf(amount: Double): Quantity<Voltage> = amount.volts

    override fun quantityOfInDefaultUnit(amount: Double): Quantity<Voltage> = amount.volts
}

// -- Milli -- //

internal class Millivolts(override val inOwnUnit: Double) : Quantity<Voltage> {
    override val unit: PhysicalUnit<Voltage> get() = Millivolt

    override fun convertToDefaultUnit(): Quantity<Voltage> = (inOwnUnit * 1000).toQuantity(Volt)
}

public val Double.millivolts: Quantity<Voltage> get() = Millivolts(this)

public object Millivolt : PhysicalUnit<Voltage> {
    public const val SYMBOL: String = "mV"

    override val default: PhysicalUnit<Voltage> get() = Volt
    override val quantityType: KClass<Voltage> get() = Voltage::class
    override val symbol: String get() = SYMBOL

    override fun quantityOf(amount: Double): Quantity<Voltage> = amount.millivolts

    override fun quantityOfInDefaultUnit(amount: Double): Quantity<Voltage> = (amount / 1000).toQuantity(this)
}

// -- Micro -- //

internal class Microvolts(override val inOwnUnit: Double) : Quantity<Voltage> {
    override val unit: PhysicalUnit<Voltage> get() = Microvolt

    override fun convertToDefaultUnit(): Quantity<Voltage> = (inOwnUnit * 1_000_000).toQuantity(Volt)
}

public val Double.microvolts: Quantity<Voltage> get() = Microvolts(this)

public object Microvolt : PhysicalUnit<Voltage> {
    public const val SYMBOL: String = "μV"

    override val default: PhysicalUnit<Voltage> get() = Volt
    override val quantityType: KClass<Voltage> get() = Voltage::class
    override val symbol: String get() = SYMBOL

    override fun quantityOf(amount: Double): Quantity<Voltage> = amount.microvolts

    override fun quantityOfInDefaultUnit(amount: Double): Quantity<Voltage> = (amount / 1_000_000).toQuantity(this)
}