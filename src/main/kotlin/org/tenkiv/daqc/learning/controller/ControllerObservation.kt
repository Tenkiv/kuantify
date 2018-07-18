package org.tenkiv.daqc.learning.controller

import org.deeplearning4j.rl4j.space.Encodable
import org.tenkiv.daqc.BinaryState

private fun BinaryState?.toDouble(): Double =
    when (this) {
        null -> -1.0
        BinaryState.Off -> 0.0
        BinaryState.On -> 1.0
    }

//TODO: Do I need to scale the observations?
data class ControllerObservation(val channelObservations: List<Encodable>) : Encodable {

    override fun toArray(): DoubleArray {

        val list = ArrayList<Double>()
        channelObservations.forEach { obs ->
            obs.toArray().forEach {
                list += it
            }
        }

        return list.toDoubleArray()
    }


}

data class BinaryStateInputObs(val measurementValue: BinaryState?, val timeSinceChange: Long) : Encodable {

    override fun toArray(): DoubleArray = doubleArrayOf(measurementValue.toDouble(), timeSinceChange.toDouble())

}

data class QuantityInputObs(val measurementValue: Double, val valueAcceleration: Double) : Encodable {

    override fun toArray(): DoubleArray = doubleArrayOf(measurementValue, valueAcceleration)

}

data class BinaryStateOutputObs(val setting: BinaryState?, val timeSinceChange: Long) : Encodable {

    override fun toArray(): DoubleArray = doubleArrayOf(setting.toDouble(), timeSinceChange.toDouble())

}

data class QuantityOutputObs(
    val setting: Double,
    val lastChange: Double,
    val timeSinceChange: Long
) : Encodable {

    override fun toArray(): DoubleArray = doubleArrayOf(setting, lastChange, timeSinceChange.toDouble())

}