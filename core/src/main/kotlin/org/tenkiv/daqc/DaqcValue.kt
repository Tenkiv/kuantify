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

import org.tenkiv.physikal.core.*
import tec.units.indriya.ComparableQuantity
import tec.units.indriya.quantity.Quantities
import java.util.zip.DataFormatException
import javax.measure.Quantity
import org.tenkiv.physikal.core.toByteInSystemUnit as physikalToByteInSystemUnit
import org.tenkiv.physikal.core.toDoubleInSystemUnit as physikalToDoubleInSystemUnit
import org.tenkiv.physikal.core.toFloatInSystemUnit as physikalToFloatInSystemUnit
import org.tenkiv.physikal.core.toIntInSystemUnit as physikalToIntInSystemUnit
import org.tenkiv.physikal.core.toLongInSystemUnit as physikalToLongInSystemUnit
import org.tenkiv.physikal.core.toShortInSystemUnit as physikalToShortInSystemUnit

sealed class DaqcValue : DaqcData {

    override val size get() = 1

    fun toShortInSystemUnit(): Short = when (this) {
        is BinaryState -> this.toShort()
        is DaqcQuantity<*> -> this.physikalToShortInSystemUnit()
    }

    fun toIntInSystemUnit(): Int = when (this) {
        is BinaryState -> this.toInt()
        is DaqcQuantity<*> -> this.physikalToIntInSystemUnit()
    }

    fun toLongInSystemUnit(): Long = when (this) {
        is BinaryState -> this.toLong()
        is DaqcQuantity<*> -> this.physikalToLongInSystemUnit()
    }

    fun toByteInSystemUnit(): Byte = when (this) {
        is BinaryState -> this.toByte()
        is DaqcQuantity<*> -> this.physikalToByteInSystemUnit()
    }

    fun toFloatInSystemUnit(): Float = when (this) {
        is BinaryState -> this.toFloat()
        is DaqcQuantity<*> -> this.physikalToFloatInSystemUnit()
    }

    fun toDoubleInSystemUnit(): Double = when (this) {
        is BinaryState -> this.toDouble()
        is DaqcQuantity<*> -> this.physikalToDoubleInSystemUnit()
    }

    override fun toDaqcValueList() = listOf(this)

}

sealed class BinaryState : DaqcValue(), Comparable<BinaryState> {

    abstract fun toShort(): Short

    abstract fun toInt(): Int

    abstract fun toLong(): Long

    abstract fun toByte(): Byte

    abstract fun toFloat(): Float

    abstract fun toDouble(): Double

    // There are only ever 2 possible ranges of binary states.
    operator fun rangeTo(other: BinaryState): ClosedRange<BinaryState> =
        when (this) {
            Off -> FullBinaryStateRange
            On -> EmptyBinaryStateRange
        }

    object On : BinaryState() {

        private const val SHORT_REPRESENTATION: Short = 1
        private const val BYTE_REPRESENTATION: Byte = 1

        override fun compareTo(other: BinaryState) =
            when (other) {
                is On -> 0
                is Off -> 1
            }

        override fun toShort() = SHORT_REPRESENTATION

        override fun toInt() = 1

        override fun toLong() = 1L

        override fun toByte() = BYTE_REPRESENTATION

        override fun toFloat() = 1f

        override fun toDouble() = 1.0

        override fun toString() = "BinaryState.ON"
    }

    object Off : BinaryState() {

        private const val SHORT_REPRESENTATION: Short = 0
        private const val BYTE_REPRESENTATION: Byte = 0

        override fun compareTo(other: BinaryState) =
            when (other) {
                is On -> -1
                is Off -> 0
            }

        override fun toShort() = SHORT_REPRESENTATION

        override fun toInt() = 0

        override fun toLong() = 0L

        override fun toByte() = BYTE_REPRESENTATION

        override fun toFloat() = 0f

        override fun toDouble() = 0.0

        override fun toString() = "BinaryState.OFF"

    }

    companion object {

        val range: ClosedRange<BinaryState> get() = FullBinaryStateRange

        fun fromString(input: String): BinaryState {
            if (input == BinaryState.On.toString()) {
                return BinaryState.On
            }
            if (input == BinaryState.Off.toString()) {
                return BinaryState.Off
            }
            throw DataFormatException("Data with BinaryState not found")
        }
    }

}

private object FullBinaryStateRange : ClosedRange<BinaryState> {
    override val endInclusive get() = BinaryState.On
    override val start get() = BinaryState.Off

    override fun contains(value: BinaryState) = true

    override fun isEmpty() = false
}

private object EmptyBinaryStateRange : ClosedRange<BinaryState> {
    override val endInclusive get() = BinaryState.Off
    override val start get() = BinaryState.On

    override fun contains(value: BinaryState) = false

    override fun isEmpty() = true
}

class DaqcQuantity<Q : Quantity<Q>>(private val wrappedQuantity: ComparableQuantity<Q>) : DaqcValue(),
    ComparableQuantity<Q> by wrappedQuantity {

    override fun toString() = wrappedQuantity.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as DaqcQuantity<*>

        if (wrappedQuantity != other.wrappedQuantity) return false

        return true
    }

    override fun hashCode(): Int {
        return wrappedQuantity.hashCode()
    }


    companion object {
        fun <Q : Quantity<Q>> of(value: Number, unit: PhysicalUnit<Q>) =
            DaqcQuantity(Quantities.getQuantity(value, unit))

        fun <Q : Quantity<Q>> of(quantity: ComparableQuantity<Q>) =
            DaqcQuantity(quantity)

        fun <Q : Quantity<Q>> of(measurement: QuantityMeasurement<Q>) =
            DaqcQuantity(measurement.value)

        inline fun <reified Q : Quantity<Q>> fromString(input: String): DaqcQuantity<Q> {
            val quant: ComparableQuantity<Q>? = Quantities.getQuantity(
                input
            ).asType()
            if (quant != null) {
                return DaqcQuantity.of(quant)
            } else {
                throw DataFormatException("Data with Quantity value not found")
            }
        }
    }
}