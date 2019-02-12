/*
 * Copyright 2019 Tenkiv, Inc.
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
 *
 */

package org.tenkiv.kuantify.data.vector

import org.tenkiv.kuantify.data.*
import org.tenkiv.physikal.core.*
import org.tenkiv.physikal.si.*
import tec.units.indriya.*
import tec.units.indriya.unit.Units.*
import javax.measure.*
import javax.measure.quantity.*
import kotlin.math.*

private fun ComparableQuantity<Angle>.compassInvert(): DaqcQuantity<Angle> {
    val degree180 = 180.degreeAngle

    return if (this >= degree180) {
        (this - degree180).toDaqc()
    } else {
        (this + degree180).toDaqc()
    }
}

class PolarVector2D<Q : Quantity<Q>>(
    magnitude: ComparableQuantity<Q>,
    angle: ComparableQuantity<Angle>,
    val axisLabel: String,
    val positiveDirection: CircularDirection
) : DaqcData {
    val magnitude = magnitude.toDaqc()

    val angle = angle.toDaqc()

    override val size get() = 2

    override fun toDaqcValues() = listOf(magnitude, angle)

    /**
     * Converts this [PolarVector2D] to a [Pair] of [Double]s representing the equivalent components of a Euclidean vector in
     * the system unit.
     */
    private fun toComponentDoubles(): Pair<Double, Double> {
        val magnitude = magnitude.valueToDouble()
        val angle = angle toDoubleIn RADIAN

        val xComponent = cos(angle) * magnitude
        var yComponent = sin(angle) * magnitude
        if (positiveDirection === CircularDirection.CLOCKWISE) yComponent = -yComponent

        return Pair(xComponent, yComponent)
    }

    fun toComponents(): Components<Q> {
        val components = toComponentDoubles()
        val unit = magnitude.unit

        return Components(components.first(unit), components.second(unit))
    }

    infix fun compassScale(scalar: Double): PolarVector2D<Q> {
        val negativeScalar = scalar < 0
        val scaledMagnitude = magnitude * scalar.absoluteValue
        val angle = if (negativeScalar) angle.compassInvert() else angle

        return PolarVector2D(scaledMagnitude, angle, axisLabel, positiveDirection)
    }

    operator fun times(scalar: Double): PolarVector2D<Q> =
        PolarVector2D(magnitude * scalar, angle, axisLabel, positiveDirection)

    operator fun unaryPlus() =
        PolarVector2D(+magnitude, angle, axisLabel, positiveDirection)

    operator fun unaryMinus() =
        PolarVector2D(-magnitude, angle, axisLabel, positiveDirection)

    operator fun plus(other: PolarVector2D<Q>): PolarVector2D<Q> {
        val (thisX, thisY) = toComponents()
        val (otherX, otherY) = other.toComponents()

        val resultX = thisX + otherX
        val resultY = thisY + otherY

        return fromComponents(
            resultX,
            resultY,
            other.axisLabel,
            other.positiveDirection
        )
    }

    operator fun minus(other: PolarVector2D<Q>): PolarVector2D<Q> {
        val (thisX, thisY) = toComponents()
        val (otherX, otherY) = other.toComponents()

        val resultX = thisX - otherX
        val resultY = thisY - otherY

        return fromComponents(
            resultX,
            resultY,
            other.axisLabel,
            other.positiveDirection
        )
    }

    /**
     * @return The dot product.
     */
    inline infix fun <reified RQ : Quantity<RQ>> dot(other: PolarVector2D<*>): ComparableQuantity<RQ> {
        val (thisX, thisY) = toComponents()
        val (otherX, otherY) = other.toComponents()

        val resultX = thisX * otherX
        val resultY = thisY * otherY
        val resultUnit = resultX.getUnit()

        //TODO: Find a way to create a Quantity of unknown type without going through a string
        return (resultX.valueToDouble() + resultY.valueToDouble()).withSymbol(resultUnit.getSymbol()).asType()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PolarVector2D<*>

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
        "PolarVector2D($magnitude, $angle $positiveDirection from $axisLabel)"

    data class Components<Q : Quantity<Q>>(val x: ComparableQuantity<Q>, val y: ComparableQuantity<Q>)

    companion object {
        fun <Q : Quantity<Q>> fromComponents(
            xComponent: ComparableQuantity<Q>,
            yComponent: ComparableQuantity<Q>,
            axisLabel: String,
            positiveDirection: CircularDirection,
            unit: PhysicalUnit<Q> = xComponent.unit
        ): PolarVector2D<Q> {
            val xComponentDouble = xComponent toDoubleIn unit
            val yComponentDouble = yComponent toDoubleIn unit

            return fromComponentDoubles(xComponentDouble, yComponentDouble, axisLabel, positiveDirection, unit)
        }

        private fun <Q : Quantity<Q>> fromComponentDoubles(
            xComponent: Double,
            yComponent: Double,
            axisLabel: String,
            positiveDirection: CircularDirection,
            unit: PhysicalUnit<Q>
        ): PolarVector2D<Q> {
            val magnitude = sqrt(xComponent.pow(2) + yComponent.pow(2))(unit)
            val angle = atan2(yComponent, xComponent)(RADIAN)

            return PolarVector2D(magnitude, angle, axisLabel, positiveDirection)
        }
    }
}