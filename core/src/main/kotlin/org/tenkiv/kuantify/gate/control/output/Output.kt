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

package org.tenkiv.kuantify.gate.control.output

import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import tec.units.indriya.unit.Units.*
import javax.measure.*
import javax.measure.quantity.*

/**
 * Interface defining classes which act as outputs and send controls or signals to devices.
 *
 * @param T The type of signal given by this output.
 */
interface Output<T : DaqcValue> : IOStrand<T>, ControlGate<T>

/**
 * An [Output] which sends signals in the form of a [DaqcQuantity].
 *
 * @param Q The type of signal sent by this Output.
 */
interface QuantityOutput<Q : Quantity<Q>> : Output<DaqcQuantity<Q>> {

    /**
     * Adjust the output by applying a function to the current setting. For example double it or square it.
     * This will fail with a return [AdjustmentAttempt.UninitialisedSetting] if there hasn't yet been a setting provided
     * for this [Output]
     */
    fun adjustOutputOrFail(
        adjustment: (Double) -> Double,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult {
        val setting = valueOrNull
        return if (setting != null) {
            setOutput(adjustment(setting.value.valueToDouble())(setting.value.unit), panicOnFailure)
        } else {
            SettingResult.Failure(UninitialisedSettingException(this), panicOnFailure)
        }
    }

    /**
     * Adjust the output by applying a function to the current setting. For example double it or square it.
     * If there hasn't yet been a setting provided for this output, this function will suspend until there is one.
     */
    suspend fun adjustOutput(
        adjustment: (Double) -> Double,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult = setOutput(adjustment(getValue().value.valueToDouble())(getValue().value.unit), panicOnFailure)

}

/**
 * Sets the signal of this output to the correct value as a [DaqcQuantity].
 *
 * @param setting The signal to set as the output.
 */
fun <Q : Quantity<Q>> QuantityOutput<Q>.setOutput(
    setting: ComparableQuantity<Q>,
    panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
) =
    setOutput(setting.toDaqc(), panicOnFailure)

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
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult {
        val setting = valueOrNull

        return if (setting != null) {
            val newSetting = setting.value * ratioIncrease
            if (newSetting.toDaqc() in valueRange) {
                setOutput(setting.value * ratioIncrease, panicOnFailure)
            } else {
                SettingResult.Failure(SettingOutOfRangeException(this), panicOnFailure)
            }
        } else {
            SettingResult.Failure(UninitialisedSettingException(this), panicOnFailure)
        }
    }

    fun decreaseByRatioOfRange(
        ratioDecrease: Double,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult = increaseByRatioOfRange(-ratioDecrease, panicOnFailure)

    /**
     * Increase the setting by a percentage of the allowable range for this output.
     */
    fun increaseByPercentOfRange(
        percentIncrease: ComparableQuantity<Dimensionless>,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult = increaseByRatioOfRange(percentIncrease.toDoubleIn(PERCENT) / 100, panicOnFailure)

    /**
     * Decrease the setting by a percentage of the allowable range for this output.
     */
    fun decreaseByPercentOfRange(
        percentDecrease: ComparableQuantity<Dimensionless>,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ) = decreaseByRatioOfRange(percentDecrease.toDoubleIn(PERCENT) / 100, panicOnFailure)

    fun setOutputToPercentMaximum(
        percent: ComparableQuantity<Dimensionless>,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult =
        setOutputToRatioMaximum(percent.toDoubleIn(PERCENT) / 100, panicOnFailure)

    fun setOutputToRatioMaximum(
        ratio: Double,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult =
        setOutput(ratioOfRange(ratio), panicOnFailure)

    /**
     * Sets this output to random setting within the allowable range.
     */
    fun setOutputToRandom(panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE): SettingResult {
        val random = Math.random()

        val setting = ratioOfRange(random)

        return setOutput(setting, panicOnFailure)
    }

    private fun ratioOfRange(ratio: Double): ComparableQuantity<Q> {
        val min = valueRange.start.toDoubleInSystemUnit()
        val max = valueRange.endInclusive.toDoubleInSystemUnit()

        return (ratio * (max - min) + min)(valueRange.start.unit.systemUnit)
    }
}

class RqoAdapter<Q : Quantity<Q>> internal constructor(
    private val output: QuantityOutput<Q>,
    override val valueRange: ClosedRange<DaqcQuantity<Q>>
) : RangedQuantityOutput<Q>, QuantityOutput<Q> by output {

    override fun setOutput(setting: DaqcQuantity<Q>, panicOnFailure: Boolean): SettingResult {
        val inRange = setting in valueRange
        return if (!inRange) {
            SettingResult.Failure(SettingOutOfRangeException(this), panicOnFailure)
        } else {
            output.setOutput(setting, panicOnFailure)
        }
    }
    //TODO: ToString, equals, hashcode
}

/**
 * Converts a [QuantityOutput] to a [RqoAdapter] so it can be used in the default learning module.
 *
 * @param valueRange The range of acceptable output values.
 */
fun <Q : Quantity<Q>> QuantityOutput<Q>.toRangedOutput(valueRange: ClosedRange<DaqcQuantity<Q>>) =
    RqoAdapter(this, valueRange)