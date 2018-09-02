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

package org.tenkiv.kuantify.gate.control.output

import org.tenkiv.kuantify.DaqcException
import org.tenkiv.kuantify.data.BinaryState
import org.tenkiv.kuantify.data.DaqcQuantity
import org.tenkiv.kuantify.data.DaqcValue
import org.tenkiv.kuantify.data.toDaqc
import org.tenkiv.kuantify.gate.IOStrand
import org.tenkiv.kuantify.gate.RangedIOStrand
import org.tenkiv.kuantify.gate.control.attempt.AdjustmentAttempt
import org.tenkiv.kuantify.gate.control.attempt.RangeLimitedAttempt
import org.tenkiv.kuantify.gate.control.attempt.Viability
import org.tenkiv.physikal.core.*
import tec.units.indriya.ComparableQuantity
import tec.units.indriya.unit.Units.*
import javax.measure.Quantity
import javax.measure.quantity.Dimensionless

/**
 * Interface defining classes which act as outputs and send controls or signals to devices.
 *
 * @param T The type of signal given by this output.
 */
interface Output<T : DaqcValue> : IOStrand<T> {

    /**
     * @param throwIfNotViable If true invoking setOutput will throw an exception instead of returning a [Viability].
     */
    fun setOutput(setting: T, throwIfNotViable: Boolean = DEFAULT_THROW_IF_NOT_VIABLE): Viability


    companion object {
        const val DEFAULT_THROW_IF_NOT_VIABLE = true
    }
}

open class SettingException(open val problem: Viability) : DaqcException(problem.message)

/**
 * An [Output] which sends signals in the form of a [DaqcQuantity].
 *
 * @param Q The type of signal sent by this Output.
 */
interface QuantityOutput<Q : Quantity<Q>> : Output<DaqcQuantity<Q>> {

    /**
     * Sets the signal of this output to the correct value as a [DaqcQuantity].
     *
     * @param setting The signal to set as the output.
     */
    fun setOutput(setting: ComparableQuantity<Q>, throwIfNotViable: Boolean = Output.DEFAULT_THROW_IF_NOT_VIABLE) =
        setOutput(setting.toDaqc(), throwIfNotViable)

    /**
     * Adjust the output by applying a function to the current setting. For example double it or square it.
     * This will fail with a return [AdjustmentAttempt.UninitialisedSetting] if there hasn't yet been a setting provided
     * for this [Output]
     */
    fun adjustOutputOrFail(
        adjustment: (Double) -> Double,
        throwIfNotViable: Boolean = Output.DEFAULT_THROW_IF_NOT_VIABLE
    ): Viability {
        val setting = valueOrNull
        return if (setting != null) {
            AdjustmentAttempt.OK(
                setOutput(adjustment(setting.value.valueToDouble())(setting.value.unit), throwIfNotViable)
            )
        } else {
            AdjustmentAttempt.UninitialisedSetting.getOrThrow(throwIfNotViable)
        }
    }

    /**
     * Adjust the output by applying a function to the current setting. For example double it or square it.
     * If there hasn't yet been a setting provided for this output, this function will suspend until there is one.
     */
    suspend fun adjustOutput(
        adjustment: (Double) -> Double,
        throwIfNotViable: Boolean = Output.DEFAULT_THROW_IF_NOT_VIABLE
    ): Viability = setOutput(adjustment(getValue().value.valueToDouble())(getValue().value.unit), throwIfNotViable)

}

/**
 * An [Output] whose type extends both [DaqcValue] and [Comparable] so it can be used in the default learning module.
 */
interface RangedOutput<T> : Output<T>, RangedIOStrand<T> where T : DaqcValue, T : Comparable<T>

/**
 * A [RangedOutput] which uses the [BinaryState] type.
 */
interface BinaryStateOutput : RangedOutput<BinaryState> {

    override val valueRange get() = BinaryState.range

}

interface RangedQuantityOutput<Q : Quantity<Q>> : RangedOutput<DaqcQuantity<Q>>, QuantityOutput<Q> {

    fun increaseByRatioOfRange(
        ratioIncrease: Double,
        throwIfNotViable: Boolean = Output.DEFAULT_THROW_IF_NOT_VIABLE
    ): Viability {
        val setting = valueOrNull

        return if (setting != null) {
            val newSetting = setting.value * ratioIncrease
            if (newSetting.toDaqc() in valueRange) {
                setOutput(setting.value * ratioIncrease, throwIfNotViable)
            } else {
                RangeLimitedAttempt.OutOfRange.getOrThrow(throwIfNotViable)
            }
        } else {
            AdjustmentAttempt.UninitialisedSetting.getOrThrow(throwIfNotViable)
        }
    }

    fun decreaseByRatioOfRange(
        ratioDecrease: Double,
        throwIfNotViable: Boolean = Output.DEFAULT_THROW_IF_NOT_VIABLE
    ): Viability = increaseByRatioOfRange(-ratioDecrease, throwIfNotViable)

    /**
     * Increase the setting by a percentage of the allowable range for this output.
     */
    fun increaseByPercentOfRange(
        percentIncrease: ComparableQuantity<Dimensionless>,
        throwIfNotViable: Boolean = Output.DEFAULT_THROW_IF_NOT_VIABLE
    ): Viability = increaseByRatioOfRange(percentIncrease.toDoubleIn(PERCENT) / 100, throwIfNotViable)

    /**
     * Decrease the setting by a percentage of the allowable range for this output.
     */
    fun decreaseByPercentOfRange(
        percentDecrease: ComparableQuantity<Dimensionless>,
        throwIfNotViable: Boolean = Output.DEFAULT_THROW_IF_NOT_VIABLE
    ) = decreaseByRatioOfRange(percentDecrease.toDoubleIn(PERCENT) / 100, throwIfNotViable)

    /**
     * Sets this output to random setting within the allowable range.
     */
    fun setOutputToRandom(throwIfNotViable: Boolean = Output.DEFAULT_THROW_IF_NOT_VIABLE): Viability {
        val random = Math.random()

        val min = valueRange.start.toDoubleInSystemUnit()
        val max = valueRange.endInclusive.toDoubleInSystemUnit()

        val setting = (random * (max - min) + min)(valueRange.start.unit.systemUnit)

        return setOutput(setting, throwIfNotViable)
    }
}

class RqoAdapter<Q : Quantity<Q>> internal constructor(
    private val output: QuantityOutput<Q>,
    override val valueRange: ClosedRange<DaqcQuantity<Q>>
) : RangedQuantityOutput<Q>, QuantityOutput<Q> by output {

    override fun setOutput(setting: DaqcQuantity<Q>, throwIfNotViable: Boolean): RangeLimitedAttempt {
        val inRange = setting in valueRange
        return if (!inRange) {
            RangeLimitedAttempt.OutOfRange.getOrThrow(throwIfNotViable)
        } else {
            RangeLimitedAttempt.InRange(output.setOutput(setting, throwIfNotViable))
        }
    }
}

/**
 * Converts a [QuantityOutput] to a [RqoAdapter] so it can be used in the default learning module.
 *
 * @param valueRange The range of acceptable output values.
 */
fun <Q : Quantity<Q>> QuantityOutput<Q>.toRangedOutput(valueRange: ClosedRange<DaqcQuantity<Q>>) =
    RqoAdapter(this, valueRange)