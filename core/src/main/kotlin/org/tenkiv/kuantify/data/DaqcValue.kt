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

package org.tenkiv.kuantify.data

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import org.tenkiv.kuantify.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import tec.units.indriya.quantity.*
import java.util.zip.*
import javax.measure.*
import org.tenkiv.physikal.core.toByteInSystemUnit as physikalToByteInSystemUnit
import org.tenkiv.physikal.core.toDoubleInSystemUnit as physikalToDoubleInSystemUnit
import org.tenkiv.physikal.core.toFloatInSystemUnit as physikalToFloatInSystemUnit
import org.tenkiv.physikal.core.toIntInSystemUnit as physikalToIntInSystemUnit
import org.tenkiv.physikal.core.toLongInSystemUnit as physikalToLongInSystemUnit
import org.tenkiv.physikal.core.toShortInSystemUnit as physikalToShortInSystemUnit

/**
 * The wrapper class representing the different types of data which can be returned from a basic [Updatable].
 * Either a [BinaryState] or a [DaqcQuantity].
 */
sealed class DaqcValue : DaqcData {

    override val size get() = 1

    /**
     * Gets the value of the [DaqcValue] as a [Short] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Short].
     */
    fun toShortInSystemUnit(): Short = when (this) {
        is BinaryState -> this.toShort()
        is DaqcQuantity<*> -> this.physikalToShortInSystemUnit()
    }

    /**
     * Gets the value of the [DaqcValue] as a [Int] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Int].
     */
    fun toIntInSystemUnit(): Int = when (this) {
        is BinaryState -> this.toInt()
        is DaqcQuantity<*> -> this.physikalToIntInSystemUnit()
    }

    /**
     * Gets the value of the [DaqcValue] as a [Short] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Short].
     */
    fun toLongInSystemUnit(): Long = when (this) {
        is BinaryState -> this.toLong()
        is DaqcQuantity<*> -> this.physikalToLongInSystemUnit()
    }

    /**
     * Gets the value of the [DaqcValue] as a [Byte] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Byte].
     */
    fun toByteInSystemUnit(): Byte = when (this) {
        is BinaryState -> this.toByte()
        is DaqcQuantity<*> -> this.physikalToByteInSystemUnit()
    }

    /**
     * Gets the value of the [DaqcValue] as a [Float] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Float].
     */
    fun toFloatInSystemUnit(): Float = when (this) {
        is BinaryState -> this.toFloat()
        is DaqcQuantity<*> -> this.physikalToFloatInSystemUnit()
    }

    /**
     * Gets the value of the [DaqcValue] as a [Double] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Double].
     */
    fun toDoubleInSystemUnit(): Double = when (this) {
        is BinaryState -> this.toDouble()
        is DaqcQuantity<*> -> this.physikalToDoubleInSystemUnit()
    }

    override fun toDaqcValueList() = listOf(this)

}

/**
 * A [DaqcValue] representing a value which is either on or off.
 */
@Serializable
sealed class BinaryState : DaqcValue(), Comparable<BinaryState> {

    /**
     * Returns the binary value as a [Boolean]. This will return true for [High] and false for [Low]
     *
     * @return The binary value as a [Boolean]
     */
    abstract fun toBoolean(): Boolean

    /**
     * Returns the binary value as a [Short]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Short]
     */
    abstract fun toShort(): Short

    /**
     * Returns the binary value as a [Int]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Int]
     */
    abstract fun toInt(): Int

    /**
     * Returns the binary value as a [Long]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Long]
     */
    abstract fun toLong(): Long

    /**
     * Returns the binary value as a [Byte]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Byte]
     */
    abstract fun toByte(): Byte

    /**
     * Returns the binary value as a [Float]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Float]
     */
    abstract fun toFloat(): Float

    /**
     * Returns the binary value as a [Double]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Double]
     */
    abstract fun toDouble(): Double

    /**
     * Returns a range of [BinaryState]s. There are only two binary states.
     *
     * @return A [ClosedRange] of [BinaryState]s.
     */
    operator fun rangeTo(other: BinaryState): ClosedRange<BinaryState> =
        when (this) {
            Low -> FullBinaryStateRange
            High -> EmptyBinaryStateRange
        }

    /**
     * The [BinaryState] representing the activated value or the 1 state.
     */
    object High : BinaryState() {

        const val SHORT_REPRESENTATION: Short = 1
        const val BYTE_REPRESENTATION: Byte = 1

        override fun compareTo(other: BinaryState) =
            when (other) {
                is High -> 0
                is Low -> 1
            }

        override fun toBoolean(): Boolean = true

        override fun toShort() = SHORT_REPRESENTATION

        override fun toInt() = 1

        override fun toLong() = 1L

        override fun toByte() = BYTE_REPRESENTATION

        override fun toFloat() = 1f

        override fun toDouble() = 1.0

        override fun toString() = "BinaryState.HIGH"
    }

    /**
     * The [BinaryState] representing the deactivated value or the 0 state.
     */
    object Low : BinaryState() {

        const val SHORT_REPRESENTATION: Short = 0
        const val BYTE_REPRESENTATION: Byte = 0

        override fun compareTo(other: BinaryState) =
            when (other) {
                is High -> -1
                is Low -> 0
            }

        override fun toBoolean(): Boolean = false

        override fun toShort() = SHORT_REPRESENTATION

        override fun toInt() = 0

        override fun toLong() = 0L

        override fun toByte() = BYTE_REPRESENTATION

        override fun toFloat() = 0f

        override fun toDouble() = 0.0

        override fun toString() = "BinaryState.LOW"

    }

    @Serializer(forClass = BinaryState::class)
    companion object : KSerializer<BinaryState> {

        override val descriptor: SerialDescriptor = ByteDescriptor.withName("BinaryState")

        /**
         * The [ClosedRange] of all [BinaryState]s.
         */
        val range: ClosedRange<BinaryState> get() = FullBinaryStateRange

        /**
         * Gets a [BinaryState] value from a [String] or throws a [DataFormatException] if improperly formatted.
         *
         * @param input The string value to convert.
         * @return The [BinaryState] value of the string.
         * @throws [DataFormatException] if the [String] is malformed or not a 1 or 0.
         */
        fun fromString(input: String): BinaryState {
            if (input == High.toString()) {
                return High
            }
            if (input == Low.toString()) {
                return Low
            }
            throw DataFormatException("Data with BinaryState not found")
        }

        override fun deserialize(input: Decoder): BinaryState {
            return when (input.decodeByte()) {
                High.BYTE_REPRESENTATION -> High
                Low.BYTE_REPRESENTATION -> Low
                else -> throw SerializationException("Data with BinaryState not found")
            }
        }

        override fun serialize(output: Encoder, obj: BinaryState) {
            output.encodeByte(obj.toByte())
        }

    }

}

/**
 * The [ClosedRange] of all [BinaryState]s.
 */
private object FullBinaryStateRange : ClosedRange<BinaryState> {
    override val endInclusive get() = BinaryState.High
    override val start get() = BinaryState.Low

    override fun contains(value: BinaryState) = true

    override fun isEmpty() = false
}

/**
 * An empty [ClosedRange] containing no [BinaryState]s.
 */
private object EmptyBinaryStateRange : ClosedRange<BinaryState> {
    override val endInclusive get() = BinaryState.Low
    override val start get() = BinaryState.High

    override fun contains(value: BinaryState) = false

    override fun isEmpty() = true
}

/**
 * The [DaqcValue] representing a value which can be expressed as a [Quantity].
 */
class DaqcQuantity<Q : Quantity<Q>>(internal val wrappedQuantity: ComparableQuantity<Q>) : DaqcValue(),
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

        /**
         * Function to create a [DaqcQuantity] from a [Number] and a [Quantity].
         *
         * @param value The value of this [DaqcQuantity].
         * @param unit The [PhysicalUnit] representation of the [value]
         * @return The [value] in the form of a [DaqcQuantity] in the type of [unit].
         */
        fun <Q : Quantity<Q>> of(value: Number, unit: PhysicalUnit<Q>) =
            DaqcQuantity(Quantities.getQuantity(value, unit))

        /**
         * Function to convert a [ComparableQuantity] to a [DaqcQuantity].
         *
         * @param quantity The [Quantity] to be converted.
         * @return The [DaqcQuantity] representation of the [quantity]
         */
        fun <Q : Quantity<Q>> of(quantity: ComparableQuantity<Q>) =
            DaqcQuantity(quantity)

        /**
         * Function to create a [DaqcQuantity] from an existing [QuantityMeasurement].
         *
         * @param measurement The [QuantityMeasurement] to be converted.
         * @return The [DaqcQuantity] value.
         */
        fun <Q : Quantity<Q>> of(measurement: QuantityMeasurement<Q>) =
            DaqcQuantity(measurement.value)

        /**
         * Function to create a [DaqcQuantity] from a [String] representation.
         *
         * @param input The [String] representation of the [DaqcQuantity]
         * @return The [DaqcQuantity] value of the [String].
         * @throws DataFormatException If the [input] is malformed, invalid, or null.
         */
        inline fun <reified Q : Quantity<Q>> fromString(input: String): DaqcQuantity<Q> {
            val quantity: ComparableQuantity<Q>? = Quantities.getQuantity(
                input
            ).asTypeOrNull()
            if (quantity != null) {
                return of(quantity)
            } else {
                throw DataFormatException("Data with Quantity value not found")
            }
        }
    }
}

fun <Q : Quantity<Q>> ComparableQuantity<Q>.toDaqc() =
    DaqcQuantity(this)