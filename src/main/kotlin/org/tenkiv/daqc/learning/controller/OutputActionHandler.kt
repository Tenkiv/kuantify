package org.tenkiv.daqc.learning.controller

import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.BinaryStateOutput
import org.tenkiv.daqc.RangedQuantityOutput
import org.tenkiv.physikal.core.invoke
import org.tenkiv.physikal.core.toDoubleInSystemUnit

sealed class OutputActionHandler {

    abstract fun takeAction(action: Int)

}

class BinaryStateOutputActionHandler(private val output: BinaryStateOutput) : OutputActionHandler() {

    val actionsSet get() = setOf(0, 1, 2)

    override fun takeAction(action: Int) {
        when (action) {
            1 -> setOn()
            2 -> setOff()
        }
    }

    private fun setOn() = output.setOutput(BinaryState.On)

    private fun setOff() = output.setOutput(BinaryState.Off)

}

class QuantityOutputActionHandler(private val output: RangedQuantityOutput<*>) : OutputActionHandler() {

    val actionsSet get() = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    private val subscribtion = output.broadcastChannel.openSubscription()

    private val rangeSpan = output.possibleOutputRange.endInclusive.toDoubleInSystemUnit() -
            output.possibleOutputRange.start.toDoubleInSystemUnit()

    private val currentValue get() = output.broadcastChannel.value.value.toDoubleInSystemUnit()

    private val systemUnit get() = output.broadcastChannel.value.value.getUnit().getSystemUnit()


    override fun takeAction(action: Int) {
        when (action) {
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
        }
    }

    private fun increaseByPartOfRange(rangeRatio: Double) {

        val increase = rangeSpan * rangeRatio
        val newSetting = (currentValue + increase)(systemUnit).toDaqc()

        if (newSetting <= output.possibleOutputRange.endInclusive) output.setOutput(newSetting)

    }

    private fun decreaseByPartOfRange(rangeRatio: Double) {

        val increase = rangeSpan * rangeRatio
        val newSetting = (currentValue + increase)(systemUnit).toDaqc()

        if (newSetting >= output.possibleOutputRange.start) output.setOutput(newSetting)

    }

    companion object {
        private const val RANGE_RATIO_1 = 0.000001
        private const val RANGE_RATIO_2 = 0.0001
        private const val RANGE_RATIO_3 = 0.001
        private const val RANGE_RATIO_4 = 0.01
        private const val RANGE_RATIO_5 = 0.1
    }

}
