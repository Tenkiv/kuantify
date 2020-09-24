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

package org.tenkiv.kuantify.data

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.tenkiv.coral.*
import physikal.*

public interface DaqcValue : DaqcData {

    public override val size: UInt32 get() = 1u

    /**
     * Gets the value of the [DaqcValue] as a [Int] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Int].
     */
    public fun toInt32InDefaultUnit(): Int32

    /**
     * Gets the value of the [DaqcValue] as a [Short] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Short].
     */
    public fun toInt64InDefaultUnit(): Int64

    /**
     * Gets the value of the [DaqcValue] as a [Float] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Float].
     */
    public fun toFloat32InDefaultUnit(): Float32

    /**
     * Gets the value of the [DaqcValue] as a [Double] in the default unit representation.
     *
     * @return The value of this [DaqcValue] as a [Double].
     */
    public fun toFloat64InDefaultUnit(): Float64

    public override fun toDaqcValues(): List<DaqcValue> = listOf(this)
}

/**
 * A [DaqcValue] representing a value which is either on or off.
 */
@Serializable
@SerialName("BinaryState")
public sealed class BinaryState : DaqcValue, Comparable<BinaryState> {

    /**
     * Returns the binary value as a [Boolean]. This will return true for [High] and false for [Low]
     *
     * @return The binary value as a [Boolean]
     */
    public abstract fun toBoolean(): Boolean

    /**
     * Returns the binary value as a [Short]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Short]
     */
    public abstract fun toShort(): Short

    /**
     * Returns the binary value as a [Int]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Int]
     */
    public abstract fun toInt(): Int

    /**
     * Returns the binary value as a [Long]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Long]
     */
    public abstract fun toLong(): Long

    /**
     * Returns the binary value as a [Byte]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Byte]
     */
    public abstract fun toByte(): Byte

    /**
     * Returns the binary value as a [Float]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Float]
     */
    public abstract fun toFloat(): Float

    /**
     * Returns the binary value as a [Double]. This will always return a value 1 or 0.
     *
     * @return The binary value as a [Double]
     */
    public abstract fun toDouble(): Double

    /**
     * Returns a range of [BinaryState]s. There are only two binary states.
     *
     * @return A [ClosedRange] of [BinaryState]s.
     */
    public operator fun rangeTo(other: BinaryState): ClosedRange<BinaryState> =
        when (this) {
            Low -> FullBinaryStateRange
            High -> EmptyBinaryStateRange
        }

    /**
     * The [BinaryState] representing the activated value or the 1 state.
     */
    @Serializable
    @SerialName("High")
    public object High : BinaryState() {

        public const val SHORT_REPRESENTATION: Short = 1
        public const val BYTE_REPRESENTATION: Byte = 1

        public override fun compareTo(other: BinaryState): Int =
            when (other) {
                is High -> 0
                is Low -> 1
            }

        public override fun toBoolean(): Boolean = true

        public override fun toShort(): Short = SHORT_REPRESENTATION

        public override fun toInt(): Int = 1

        public override fun toLong(): Long = 1L

        public override fun toByte(): Byte = BYTE_REPRESENTATION

        public override fun toFloat(): Float = 1f

        public override fun toDouble(): Double = 1.0

        public override fun toString(): String = "BinaryState.HIGH"

        override fun toInt32InDefaultUnit(): Int32 = toInt()

        override fun toInt64InDefaultUnit(): Int64 = toLong()

        override fun toFloat32InDefaultUnit(): Float32 = toFloat()

        override fun toFloat64InDefaultUnit(): Float64 = toDouble()
    }

    /**
     * The [BinaryState] representing the deactivated value or the 0 state.
     */
    @Serializable
    @SerialName("Low")
    public object Low : BinaryState() {

        public const val SHORT_REPRESENTATION: Short = 0
        public const val BYTE_REPRESENTATION: Byte = 0

        public override fun compareTo(other: BinaryState): Int =
            when (other) {
                is High -> -1
                is Low -> 0
            }

        public override fun toBoolean(): Boolean = false

        public override fun toShort(): Short = SHORT_REPRESENTATION

        public override fun toInt(): Int = 0

        public override fun toLong(): Long = 0L

        public override fun toByte(): Byte = BYTE_REPRESENTATION

        public override fun toFloat(): Float = 0f

        public override fun toDouble(): Double = 0.0

        public override fun toString(): String = "BinaryState.LOW"

        override fun toInt32InDefaultUnit(): Int32 = toInt()

        override fun toInt64InDefaultUnit(): Int64 = toLong()

        override fun toFloat32InDefaultUnit(): Float32 = toFloat()

        override fun toFloat64InDefaultUnit(): Float64 = toDouble()
    }

    public companion object {
        /**
         * The [ClosedRange] of all [BinaryState]s.
         */
        public val range: ClosedRange<BinaryState> get() = FullBinaryStateRange
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

//TODO: Inline
/**
 * The [DaqcValue] representing a value which can be expressed as a [Quantity].
 */
@SerialName("DaqcQuantity")
public class DaqcQuantity<QT : Quantity<QT>>(internal val wrappedQuantity: Quantity<QT>) : DaqcValue,
    Quantity<QT> by wrappedQuantity {

    override fun toInt32InDefaultUnit(): Int32 = inDefaultUnit.toInt32()

    override fun toInt64InDefaultUnit(): Int64 = inDefaultUnit.toInt64()

    override fun toFloat32InDefaultUnit(): Float32 = inDefaultUnit.toFloat32()

    override fun toFloat64InDefaultUnit(): Float64 = inDefaultUnit.toFloat64()

    public override fun toString(): String = wrappedQuantity.toString()

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as DaqcQuantity<*>

        if (wrappedQuantity != other.wrappedQuantity) return false

        return true
    }

    public override fun hashCode(): Int {
        return wrappedQuantity.hashCode()
    }

    public companion object {
        public fun <QT : Quantity<QT>> serializer(): KSerializer<DaqcQuantity<QT>> = DaqcQuantitySerializer()
    }

}

private class DaqcQuantityRange<QT : Quantity<QT>>(
    override val start: DaqcQuantity<QT>,
    override val endInclusive: DaqcQuantity<QT>
) : ClosedFloatingPointRange<DaqcQuantity<QT>> {

    override fun lessThanOrEquals(a: DaqcQuantity<QT>, b: DaqcQuantity<QT>): Boolean = a <= b

}

public class DaqcQuantitySerializer<QT : Quantity<QT>> internal constructor() : KSerializer<DaqcQuantity<QT>> {
    public override val descriptor: SerialDescriptor = Quantity.serializer<QT>().descriptor

    public override fun deserialize(decoder: Decoder): DaqcQuantity<QT> =
        decoder.decodeSerializableValue(Quantity.serializer<QT>()).toDaqc()

    public override fun serialize(encoder: Encoder, value: DaqcQuantity<QT>) {
        encoder.encodeSerializableValue(Quantity.serializer(), value.wrappedQuantity)
    }
}

public fun <QT : Quantity<QT>> Quantity<QT>.toDaqc(): DaqcQuantity<QT> = DaqcQuantity(this)

public fun <QT : Quantity<QT>> ClosedRange<Quantity<QT>>.toDaqc():
        ClosedFloatingPointRange<DaqcQuantity<QT>> = DaqcQuantityRange(start.toDaqc(), endInclusive.toDaqc())