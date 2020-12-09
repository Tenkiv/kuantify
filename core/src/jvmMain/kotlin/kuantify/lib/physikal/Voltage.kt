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

package kuantify.lib.physikal

import kotlinx.serialization.*
import org.tenkiv.coral.*
import physikal.*
import kotlin.reflect.*

public interface Voltage : Quantity<Voltage>

// --- Base --- //

@Serializable
@SerialName(Volt.SYMBOL)
public class Volts(override val inOwnUnit: Float64) : Quantity<Voltage> {
    override val unit: PhysicalUnit<Voltage> get() = Volt

    override fun convertToDefaultUnit(): Quantity<Voltage> = this
}

public val Float64.volts: Quantity<Voltage> get() = Volts(this)

@Serializable
@SerialName(Volt.SYMBOL)
public object Volt : PhysicalUnit<Voltage> {
    public const val SYMBOL: String = "V"

    override val default: PhysicalUnit<Voltage> get() = this
    override val quantityType: KClass<Voltage> get() = Voltage::class
    override val symbol: String get() = SYMBOL

    override fun quantityOf(amount: Float64): Quantity<Voltage> = amount.volts

    override fun quantityOfInDefaultUnit(amount: Float64): Quantity<Voltage> = amount.volts
}

// -- Milli -- //

@Serializable
@SerialName(Millivolt.SYMBOL)
public class Millivolts(override val inOwnUnit: Float64) : Quantity<Voltage> {
    override val unit: PhysicalUnit<Voltage> get() = Millivolt

    override fun convertToDefaultUnit(): Quantity<Voltage> = (inOwnUnit * 1000).toQuantity(Volt)
}

public val Float64.millivolts: Quantity<Voltage> get() = Millivolts(this)

@Serializable
@SerialName(Millivolt.SYMBOL)
public object Millivolt : PhysicalUnit<Voltage> {
    public const val SYMBOL: String = "mV"

    override val default: PhysicalUnit<Voltage> get() = Volt
    override val quantityType: KClass<Voltage> get() = Voltage::class
    override val symbol: String get() = SYMBOL

    override fun quantityOf(amount: Float64): Quantity<Voltage> = amount.millivolts

    override fun quantityOfInDefaultUnit(amount: Float64): Quantity<Voltage> = (amount / 1000).toQuantity(this)
}

// -- Micro -- //

@Serializable
@SerialName(Microvolt.SYMBOL)
public class Microvolts(override val inOwnUnit: Float64) : Quantity<Voltage> {
    override val unit: PhysicalUnit<Voltage> get() = Microvolt

    override fun convertToDefaultUnit(): Quantity<Voltage> = (inOwnUnit * 1_000_000).toQuantity(Volt)
}

public val Float64.microvolts: Quantity<Voltage> get() = Microvolts(this)

@Serializable
@SerialName(Microvolt.SYMBOL)
public object Microvolt : PhysicalUnit<Voltage> {
    public const val SYMBOL: String = "μV"

    override val default: PhysicalUnit<Voltage> get() = Volt
    override val quantityType: KClass<Voltage> get() = Voltage::class
    override val symbol: String get() = SYMBOL

    override fun quantityOf(amount: Float64): Quantity<Voltage> = amount.microvolts

    override fun quantityOfInDefaultUnit(amount: Float64): Quantity<Voltage> = (amount / 1_000_000).toQuantity(this)
}