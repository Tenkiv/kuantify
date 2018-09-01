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

package org.tenkiv.kuantify.data.vector

import org.tenkiv.kuantify.data.DaqcData
import org.tenkiv.kuantify.data.toDaqc
import org.tenkiv.physikal.core.*
import tec.units.indriya.ComparableQuantity
import tec.units.indriya.unit.Units.*
import javax.measure.Quantity
import javax.measure.quantity.Angle
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

open class DynPolarVector3D(
    open val magnitude: ComparableQuantity<*>,
    open val azimuth: ComparableQuantity<Angle>,
    open val incline: ComparableQuantity<Angle>,
    val azimuthAxisLabel: String,
    val inclineAxisLabel: String,
    val azimuthPositiveDirection: CircularDirection,
    val inclinePositiveDirection: CircularDirection
) {
    inline fun <reified Q : Quantity<Q>> asType(): PolarVector3D<Q> = PolarVector3D(
        magnitude.asType(),
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    internal fun toComponentDoubles(): Triple<Double, Double, Double> {
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

    open fun toComponents(): Components {
        val components = toComponentDoubles()
        val unit = magnitude.getUnit()

        //TODO: replace method of making quantities from unknown unit types
        val x = components.first withSymbol unit.getSymbol()
        val y = components.second withSymbol unit.getSymbol()
        val z = components.third withSymbol unit.getSymbol()

        return Components(x, y, z)
    }

    open operator fun times(scalar: Double) = DynPolarVector3D(
        magnitude dynTimes scalar,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    open operator fun unaryPlus() = DynPolarVector3D(
        magnitude.dynUnaryPlus(),
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    open operator fun unaryMinus() = DynPolarVector3D(
        magnitude.dynUnaryMinus(),
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    infix fun dot(other: DynPolarVector3D): ComparableQuantity<*> {
        val thisAzimuth = azimuth toDoubleIn RADIAN
        val otherAzimuth = other.azimuth toDoubleIn RADIAN
        val thisIncline = incline toDoubleIn RADIAN
        val otherIncline = other.incline toDoubleIn RADIAN

        return (magnitude * other.magnitude) dynTimes
                (sin(thisAzimuth) * sin(otherAzimuth) * cos(thisIncline - otherIncline) +
                        cos(thisAzimuth) * cos(otherAzimuth))
    }

    infix fun cross(other: DynPolarVector3D): DynPolarVector3D {
        val (thisX, thisY, thisZ) = toComponents()
        val (otherX, otherY, otherZ) = other.toComponents()

        val resultX = thisY
    }

    open class Components(
        open val x: ComparableQuantity<*>,
        open val y: ComparableQuantity<*>,
        open val z: ComparableQuantity<*>
    ) {
        inline fun <reified Q : Quantity<Q>> asType(): PolarVector3D.Components<Q> =
            PolarVector3D.Components(x.asType(), y.asType(), z.asType())

        open operator fun component1() = x
        open operator fun component2() = y
        open operator fun component3() = z

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Components

            if (x != other.x) return false
            if (y != other.y) return false
            if (z != other.z) return false

            return true
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + z.hashCode()
            return result
        }

        override fun toString(): String {
            return "Components(x=$x, y=$y, z=$z)"
        }

    }
}

/**
 * A [PolarVector3D] is a vector described in terms of two perpendicular polar planes with the same center point.
 */
class PolarVector3D<Q : Quantity<Q>>(
    magnitude: ComparableQuantity<Q>,
    azimuth: ComparableQuantity<Angle>,
    incline: ComparableQuantity<Angle>,
    azimuthAxisLabel: String,
    inclineAxisLabel: String,
    azimuthPositiveDirection: CircularDirection,
    inclinePositiveDirection: CircularDirection
) : DynPolarVector3D(
    magnitude,
    azimuth,
    incline,
    azimuthAxisLabel,
    inclineAxisLabel,
    azimuthPositiveDirection,
    inclinePositiveDirection
), DaqcData {
    override val magnitude = magnitude.toDaqc()

    override val azimuth = azimuth.toDaqc()
    override val incline = incline.toDaqc()

    override val size get() = 3

    override fun toDaqcValueList() = listOf(magnitude, incline, azimuth)

    override fun toComponents(): Components<Q> {
        val components = toComponentDoubles()
        val unit = magnitude.unit

        return Components(components.first(unit), components.second(unit), components.third(unit))
    }

    override operator fun times(scalar: Double) = PolarVector3D(
        magnitude * scalar,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    override operator fun unaryPlus() = PolarVector3D(
        +magnitude,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    override operator fun unaryMinus() = PolarVector3D(
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

    class Components<Q : Quantity<Q>>(
        override val x: ComparableQuantity<Q>,
        override val y: ComparableQuantity<Q>,
        override val z: ComparableQuantity<Q>
    ) : DynPolarVector3D.Components(x, y, z) {

        override operator fun component1() = x
        override operator fun component2() = y
        override operator fun component3() = z

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as Components<*>

            if (x != other.x) return false
            if (y != other.y) return false
            if (z != other.z) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + z.hashCode()
            return result
        }

        override fun toString(): String {
            return "Components(x=$x, y=$y, z=$z)"
        }

    }

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