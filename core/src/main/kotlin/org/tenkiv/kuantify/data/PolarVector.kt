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

import org.tenkiv.physikal.core.*
import org.tenkiv.physikal.si.degreeAngle
import si.uom.SI.*
import tec.units.indriya.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Angle
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

    override fun toDaqcValueList() = listOf(magnitude, angle)

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

    operator fun unaryPlus() = PolarVector2D(+magnitude, angle, axisLabel, positiveDirection)

    operator fun unaryMinus() = PolarVector2D(-magnitude, angle, axisLabel, positiveDirection)

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

    operator fun times(other: PolarVector2D<*>): ComparableQuantity<*> {
        val (thisX, thisY) = toComponents()
        val (otherX, otherY) = other.toComponents()

        val resultX = thisX * otherX
        val resultY = thisY * otherY
        val resultUnit = resultX.getUnit()


        //TODO: Find a way to create a Quantity of unknown type without going through a string
        return (resultX.valueToDouble() + resultY.valueToDouble()) withSymbol resultUnit.getSymbol()
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

enum class CircularDirection {
    CLOCKWISE,
    COUNTER_CLOCKWISE
}

/**
 * A [PolarVector3D] is a vector described in terms of two perpendicular polar planes with the same center point.
 */
class PolarVector3D<Q : Quantity<Q>>(
    magnitude: ComparableQuantity<Q>,
    azimuth: ComparableQuantity<Angle>,
    incline: ComparableQuantity<Angle>,
    val azimuthAxisLabel: String,
    val inclineAxisLabel: String,
    val azimuthPositiveDirection: CircularDirection,
    val inclinePositiveDirection: CircularDirection
) : DaqcData {
    val magnitude = magnitude.toDaqc()

    val azimuth = azimuth.toDaqc()
    val incline = incline.toDaqc()

    override val size get() = 3

    override fun toDaqcValueList() = listOf(magnitude, incline, azimuth)

    private fun toComponentDoubles(): Triple<Double, Double, Double> {
        val magnitude = magnitude.valueToDouble()
        val incline = incline toDoubleIn RADIAN
        val azimuth = azimuth toDoubleIn RADIAN

        val xComponent = magnitude * cos(incline) * sin(azimuth)
        var yComponent = magnitude * cos(incline) * cos(azimuth)
        var zComponent = magnitude * sin(incline)

        if (azimuthPositiveDirection === CircularDirection.CLOCKWISE) yComponent = -yComponent
        if (inclinePositiveDirection === CircularDirection.CLOCKWISE) zComponent = -zComponent

        return Triple(xComponent, yComponent, zComponent)
    }

    fun toComponents(): Components<Q> {
        val components = toComponentDoubles()
        val unit = magnitude.unit

        return Components(components.first(unit), components.second(unit), components.third(unit))
    }

    operator fun times(scalar: Double): PolarVector3D<Q> = PolarVector3D(
        magnitude * scalar,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    operator fun unaryPlus() = PolarVector3D<Q>(
        +magnitude,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    operator fun unaryMinus() = PolarVector3D<Q>(
        -magnitude,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    operator fun plus(other: PolarVector3D<Q>): PolarVector3D<Q> {
        val (thisX, thisY, thisZ) = toComponents()
        val (otherX, otherY, otherZ) = other.toComponents()

        val resultX = thisX + otherX
        val resultY = thisY + otherY
        val resultZ = thisZ + otherZ

        return fromComponents(
            resultX,
            resultY,
            resultZ,
            other.azimuthAxisLabel,
            other.inclineAxisLabel,
            other.azimuthPositiveDirection,
            other.inclinePositiveDirection
        )
    }

    operator fun minus(other: PolarVector3D<Q>): PolarVector3D<Q> {
        val (thisX, thisY, thisZ) = toComponents()
        val (otherX, otherY, otherZ) = other.toComponents()

        val resultX = thisX - otherX
        val resultY = thisY - otherY
        val resultZ = thisZ - otherZ

        return fromComponents(
            resultX,
            resultY,
            resultZ,
            other.azimuthAxisLabel,
            other.inclineAxisLabel,
            other.azimuthPositiveDirection,
            other.inclinePositiveDirection
        )
    }

    data class Components<Q : Quantity<Q>>(
        val x: ComparableQuantity<Q>,
        val y: ComparableQuantity<Q>,
        val z: ComparableQuantity<Q>
    )

    companion object {

        private fun <Q : Quantity<Q>> fromComponentDoubles(
            xComponent: Double,
            yComponent: Double,
            zComponent: Double,
            azimuthAxisLabel: String,
            inclineAxisLabel: String,
            azimuthPositiveDirection: CircularDirection,
            inclinePositiveDirection: CircularDirection,
            unit: PhysicalUnit<Q>
        ): PolarVector3D<Q> {
            val azimuthAngle = atan(xComponent / yComponent)
            val inclineAngle = acos(yComponent / zComponent)
            val magnitude = zComponent / sin(azimuthAngle)

            return PolarVector3D(
                magnitude(unit),
                azimuthAngle(RADIAN),
                inclineAngle(RADIAN),
                azimuthAxisLabel,
                inclineAxisLabel,
                azimuthPositiveDirection,
                inclinePositiveDirection
            )
        }

        fun <Q : Quantity<Q>> fromComponents(
            xComponent: ComparableQuantity<Q>,
            yComponent: ComparableQuantity<Q>,
            zComponent: ComparableQuantity<Q>,
            azimuthAxisLabel: String,
            inclineAxisLabel: String,
            azimuthPositiveDirection: CircularDirection,
            inclinePositiveDirection: CircularDirection,
            unit: PhysicalUnit<Q> = xComponent.unit
        ): PolarVector3D<Q> {
            val xComponentDouble = xComponent toDoubleIn unit
            val yComponentDouble = yComponent toDoubleIn unit
            val zComponentDouble = zComponent toDoubleIn unit

            return fromComponentDoubles(
                xComponentDouble,
                yComponentDouble,
                zComponentDouble,
                azimuthAxisLabel,
                inclineAxisLabel,
                azimuthPositiveDirection,
                inclinePositiveDirection,
                unit
            )
        }
    }
}