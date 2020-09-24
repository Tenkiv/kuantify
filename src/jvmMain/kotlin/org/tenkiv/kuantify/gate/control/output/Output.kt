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

package org.tenkiv.kuantify.gate.control.output

import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.control.*
import physikal.*
import physikal.types.*

/**
 * Interface defining classes which act as outputs and send controls or signals to devices.
 *
 * @param T The type of signal given by this output.
 */
public interface Output<T : DaqcValue> : IOStrand<T>, ControlChannel<T>

/**
 * An [Output] which sends signals in the form of a [DaqcQuantity].
 *
 * @param QT The type of signal sent by this Output.
 */
public interface QuantityOutput<QT : Quantity<QT>> : Output<DaqcQuantity<QT>> {

    /**
     * Adjust the output by applying a function to the current setting. For example double it or square it.
     * This will fail with a return [AdjustmentAttempt.UninitialisedSetting] if there hasn't yet been a setting provided
     * for this [Output]
     */
    public fun adjustOutputIfInitialized(
        adjustment: (Float64) -> Float64
    ): SettingViability {
        val setting = valueOrNull
        return if (setting != null) {
            val quantity = setting.value
            setOutputIfViable(adjustment(quantity.inOwnUnit).toQuantity(quantity.unit))
        } else {
            UninitialisedSetting()
        }
    }

    /**
     * Adjust the output by applying a function to the current setting. For example double it or square it.
     * If there hasn't yet been a setting provided for this output, this function will suspend until there is one.
     */
    public suspend fun adjustOutput(
        adjustment: (Float64) -> Float64
    ): SettingViability {
        val quantity = getValue().value
        return setOutputIfViable(adjustment(quantity.inOwnUnit).toQuantity(quantity.unit))
    }

}

/**
 * Sets the signal of this output to the correct value as a [DaqcQuantity].
 *
 * @param setting The signal to set as the output.
 */
public fun <QT : Quantity<QT>> QuantityOutput<QT>.setOutputIfViable(
    setting: Quantity<QT>
): SettingViability = setOutputIfViable(setting.toDaqc())

public fun <QT : Quantity<QT>> QuantityOutput<QT>.setOutput(setting: Quantity<QT>) {
    setOutputIfViable(setting).throwIfUnviable()
}

/**
 * An [Output] whose type extends both [DaqcValue] and [Comparable] so it can be used in the default learning module.
 */
public interface RangedOutput<T> : Output<T>, RangedIOStrand<T> where T : DaqcValue, T : Comparable<T>

/**
 * A [RangedOutput] which uses the [BinaryState] type.
 */
public interface BinaryStateOutput : RangedOutput<BinaryState> {

    override val valueRange: ClosedRange<BinaryState> get() = BinaryState.range

}

public interface RangedQuantityOutput<Q : Quantity<Q>> : RangedOutput<DaqcQuantity<Q>>, QuantityOutput<Q> {

    public fun increaseByRatioOfRange(
        ratioIncrease: Float64
    ): SettingViability {
        val setting = valueOrNull

        return if (setting != null) {
            val newSetting = setting.value * ratioIncrease
            if (newSetting.toDaqc() in valueRange) {
                setOutputIfViable(setting.value * ratioIncrease)
            } else {
                SettingOutOfRange()
            }
        } else {
            UninitialisedSetting()
        }
    }

    public fun decreaseByRatioOfRange(
        ratioDecrease: Float64
    ): SettingViability = increaseByRatioOfRange(-ratioDecrease)

    /**
     * Increase the setting by a percentage of the allowable range for this output.
     */
    public fun increaseByPercentOfRange(
        percentIncrease: Quantity<Dimensionless>
    ): SettingViability = increaseByRatioOfRange(percentIncrease.toFloat64In(Percent) / 100)

    /**
     * Decrease the setting by a percentage of the allowable range for this output.
     */
    public fun decreaseByPercentOfRange(
        percentDecrease: Quantity<Dimensionless>
    ): SettingViability = decreaseByRatioOfRange(percentDecrease.toFloat64In(Percent) / 100)

    public fun setOutputToPercentMaximum(
        percent: Quantity<Dimensionless>
    ): SettingViability = setOutputToRatioMaximum(percent.toFloat64In(Percent) / 100)

    public fun setOutputToRatioMaximum(
        ratio: Float64
    ): SettingViability = setOutputIfViable(ratioOfRange(ratio))

    /**
     * Sets this output to random setting within the allowable range.
     */
    public fun setOutputToRandom(): SettingViability {
        val random = Math.random()

        val setting = ratioOfRange(random)

        return setOutputIfViable(setting)
    }

    private fun ratioOfRange(ratio: Float64): Quantity<Q> {
        val min = valueRange.start.toFloat64InDefaultUnit()
        val max = valueRange.endInclusive.toFloat64InDefaultUnit()

        return (ratio * (max - min) + min).toQuantity(valueRange.start.unit.default)
    }
}

public class RqoAdapter<Q : Quantity<Q>> internal constructor(
    private val output: QuantityOutput<Q>,
    public override val valueRange: ClosedRange<DaqcQuantity<Q>>
) : RangedQuantityOutput<Q>, QuantityOutput<Q> by output {

    public override fun setOutputIfViable(setting: DaqcQuantity<Q>): SettingViability {
        val inRange = setting in valueRange
        return if (!inRange) {
            SettingOutOfRange()
        } else {
            output.setOutputIfViable(setting)
        }
    }
    //TODO: ToString, equals, hashcode
}

/**
 * Converts a [QuantityOutput] to a [RqoAdapter] so it can be used in the default learning module.
 *
 * @param valueRange The range of acceptable output values.
 */
public fun <Q : Quantity<Q>> QuantityOutput<Q>.toRangedOutput(valueRange: ClosedRange<DaqcQuantity<Q>>): RqoAdapter<Q> =
    RqoAdapter(this, valueRange)