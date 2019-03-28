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
 */

package org.tenkiv.kuantify.learning.controller

import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.control.output.*

internal sealed class OutputActionHandler {

    abstract val actionSet: Set<Int>

    abstract fun takeAction(action: Int)

}

internal class BinaryStateOutputActionHandler(private val output: BinaryStateOutput) : OutputActionHandler() {

    override val actionSet get() = setOf(0, 1, 2)

    override fun takeAction(action: Int) {
        when (action) {
            0 -> Unit
            1 -> setOn()
            2 -> setOff()
            else -> throw InvalidActionException("The action attempted by the MDP is invalid.")
        }
    }

    private fun setOn() = output.setOutput(BinaryState.High)

    private fun setOff() = output.setOutput(BinaryState.Low)

}

internal class QuantityOutputActionHandler(private val output: RangedQuantityOutput<*>) : OutputActionHandler() {

    override val actionSet get() = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    private val subscribtion = output.updateBroadcaster.openSubscription()

    private val rangeSpanSystemUnit = output.valueRange.endInclusive.toDoubleInSystemUnit() -
            output.valueRange.start.toDoubleInSystemUnit()

    private val currentValueSystemUnit
        get() = output.valueOrNull?.value?.toDoubleInSystemUnit()
            ?: output.valueRange.start.toDoubleInSystemUnit()

    override fun takeAction(action: Int) {
        when (action) {
            0 -> Unit
            1 -> decreaseByPartOfRange(RANGE_RATIO_5)
            2 -> decreaseByPartOfRange(RANGE_RATIO_4)
            3 -> decreaseByPartOfRange(RANGE_RATIO_3)
            4 -> decreaseByPartOfRange(RANGE_RATIO_2)
            5 -> decreaseByPartOfRange(RANGE_RATIO_1)
            6 -> increaseByPartOfRange(RANGE_RATIO_1)
            7 -> increaseByPartOfRange(RANGE_RATIO_2)
            8 -> increaseByPartOfRange(RANGE_RATIO_3)
            9 -> increaseByPartOfRange(RANGE_RATIO_4)
            10 -> increaseByPartOfRange(RANGE_RATIO_5)
            else -> throw InvalidActionException("The action attempted by the MDP is invalid.")
        }
    }

    private fun increaseByPartOfRange(rangeRatio: Double) =
        output.increaseByRatioOfRange(rangeRatio)

    private fun decreaseByPartOfRange(rangeRatio: Double) =
        output.decreaseByRatioOfRange(rangeRatio)


    companion object {
        private const val RANGE_RATIO_1 = 0.000001
        private const val RANGE_RATIO_2 = 0.0001
        private const val RANGE_RATIO_3 = 0.001
        private const val RANGE_RATIO_4 = 0.01
        private const val RANGE_RATIO_5 = 0.1
    }

}

internal class InvalidActionException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)
