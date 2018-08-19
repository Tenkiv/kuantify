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

package org.tenkiv.daqc.data

import org.tenkiv.physikal.core.*
import org.tenkiv.physikal.si.degreeAngle
import si.uom.NonSI.DEGREE_ANGLE
import si.uom.SI.*
import tec.units.indriya.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Angle
import kotlin.math.*

private const val DEFAULT_X_LABEL = "X"
private const val DEFAULT_Z_LABEL = "Z"

private fun ComparableQuantity<Angle>.compassInvert(): DaqcQuantity<Angle> {
    val degree180 = 180.degreeAngle

    return if (this >= degree180) {
        (this - degree180).toDaqc()
    } else {
        (this + degree180).toDaqc()
    }
}

class Vector<Q : Quantity<Q>>(
    magnitude: ComparableQuantity<Q>,
    angle: ComparableQuantity<Angle>,
    val axisLabel: String,
    val positiveDirection: PolarDirection = PolarDirection.COUNTER_CLOCKWISE
) : DaqcData {
    val magnitude = magnitude.toDaqc()

    val angle = angle.toDaqc()

    override val size get() = 2

    override fun toDaqcValueList() = listOf(magnitude, angle)

    /**
     * Converts this [Vector] to a [DoubleArray] representing the equivalent components of a Euclidean vector in the
     * system unit.
     */
    fun toComponentDoubles(): Pair<Double, Double> {
        val magnitude = this.magnitude.toDoubleInSystemUnit()
        val angle = this.angle toDoubleIn RADIAN

        return Pair(cos(angle) * magnitude, sin(angle) * magnitude)
    }

    fun toComponents(): Pair<ComparableQuantity<Q>, ComparableQuantity<Q>> {
        val components = toComponentDoubles()
        val unit = magnitude.unit

        return Pair(components.first(unit), components.second(unit))
    }

    infix fun compassScale(scalar: Double): Vector<Q> {
        val negativeScalar = scalar < 0
        val scaledMagnitude = magnitude * scalar.absoluteValue
        val angle = if (negativeScalar) this.angle.compassInvert() else this.angle

        return Vector(scaledMagnitude, angle, axisLabel, positiveDirection)
    }

    operator fun times(scalar: Double): Vector<Q> =
        Vector(magnitude * scalar, angle, axisLabel, positiveDirection)

    operator fun unaryPlus() = Vector(+magnitude, angle, axisLabel, positiveDirection)

    operator fun unaryMinus() = Vector(-magnitude, angle, axisLabel, positiveDirection)

    operator fun plus(other: Vector<Q>): Vector<Q> {
        val (thisX, thisY) = toComponents()
        val (otherX, otherY) = other.toComponents()

        val resultX = thisX + otherX
        val resultY = thisY + otherY

        return Vector.fromComponents(
            resultX,
            resultY,
            other.axisLabel,
            resultX.unit,
            other.positiveDirection
        )
    }

    operator fun minus(other: Vector<Q>): Vector<Q> {
        val (thisX, thisY) = toComponents()
        val (otherX, otherY) = other.toComponents()

        val resultX = thisX - otherX
        val resultY = thisY - otherY

        return Vector.fromComponents(
            resultX,
            resultY,
            other.axisLabel,
            resultX.unit,
            other.positiveDirection
        )
    }

    operator fun times(other: Vector<*>): ComparableQuantity<*> {
        val (thisX, thisY) = toComponents()
        val (otherX, otherY) = other.toComponents()

        val resultX = thisX * otherX
        val resultY = thisY * otherY
        val resultUnit = resultX.getUnit()


        //TODO: Find a way to create a Quantity of unknown type without going through a string
        return (resultX.valueToDouble() + resultY.valueToDouble()).toQuantityWithSymbol(resultUnit.getSymbol())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vector<*>

        if (axisLabel != other.axisLabel) return false
        if (positiveDirection != other.positiveDirection) return false
        if (magnitude != other.magnitude) return false
        if (angle != other.angle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = axisLabel.hashCode()
        result = 31 * result + positiveDirection.hashCode()
        result = 31 * result + magnitude.hashCode()
        result = 31 * result + angle.hashCode()
        return result
    }

    override fun toString() =
        "Vector($magnitude, $angle $positiveDirection from $axisLabel)"

    data class Components<Q : Quantity<Q>>(val x: ComparableQuantity<Q>, val y: ComparableQuantity<Q>)

    companion object {
        fun <Q : Quantity<Q>> fromComponents(
            xComponent: ComparableQuantity<Q>,
            yComponent: ComparableQuantity<Q>,
            axisLabel: String,
            unit: PhysicalUnit<Q> = xComponent.unit.systemUnit,
            positiveDirection: PolarDirection = PolarDirection.COUNTER_CLOCKWISE
        ): Vector<Q> {
            val xComponentDouble = xComponent.toDoubleInSystemUnit()
            val yComponentDouble = yComponent.toDoubleInSystemUnit()

            return fromComponentDoubles(xComponentDouble, yComponentDouble, axisLabel, unit, positiveDirection)
        }

        private fun <Q : Quantity<Q>> fromComponentDoubles(
            xComponent: Double,
            yComponent: Double,
            axisLabel: String,
            unit: PhysicalUnit<Q>,
            positiveDirection: PolarDirection = PolarDirection.COUNTER_CLOCKWISE
        ): Vector<Q> {
            val magnitude = sqrt(xComponent.pow(2) + yComponent.pow(2))(unit)
            val angle = atan(yComponent / xComponent)(DEGREE_ANGLE)

            return Vector(magnitude, angle, axisLabel, positiveDirection)
        }
    }
}

enum class PolarDirection {
    CLOCKWISE,
    COUNTER_CLOCKWISE
}

class SphericalVector<Q : Quantity<Q>>(
    magnitude: ComparableQuantity<Q>,
    xAngle: ComparableQuantity<Angle>,
    zAngle: ComparableQuantity<Angle>,
    val xLabel: String = DEFAULT_X_LABEL,
    val zLabel: String = DEFAULT_Z_LABEL
) : DaqcData {
    val magnitude = magnitude.toDaqc()

    val xAngle = xAngle.toDaqc()
    val zAngle = zAngle.toDaqc()

    override val size get() = 3

    override fun toDaqcValueList() = listOf(magnitude, xAngle, zAngle)
}