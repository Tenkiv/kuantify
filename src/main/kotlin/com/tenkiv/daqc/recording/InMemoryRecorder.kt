package com.tenkiv.daqc.recording

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.LimitedArrayList
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import tec.uom.se.unit.Units
import java.time.Instant
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 5/15/17.
 */
abstract class InMemoryRecorder<T: DaqcValue>(val updatable: Updatable<T>, val maximumSize: Int = 10000, name: String = "") : Recorder<T>(null, mapOf(Pair(updatable, name))) {

    private val _dataMap: LimitedArrayList<Pair<Instant, T?>> = LimitedArrayList(maximumSize)

    val dataMap: List<Pair<Instant, T?>>
        get() = ArrayList(_dataMap)

    override val onDataUpdate = object: UpdatableListener<T>{
        override fun onUpdate(updatedObject: Updatable<T>) {
            _dataMap.add(Pair(Instant.now(),updatedObject.value))
        }
    }
}

class AnalogMemoryRecorder(updatable: Updatable<DaqcValue.Quantity<ElectricPotential>>,
                           maximumSize: Int,
                           name: String): InMemoryRecorder<DaqcValue.Quantity<ElectricPotential>>(updatable, maximumSize, name) {

    fun average(): DaqcValue.Quantity<ElectricPotential>{
        var sum = 0.0
        val data = dataMap
        data.forEach {
            sum += (it.second ?: DaqcValue.Quantity.of(0,Units.VOLT)).to(Units.VOLT).value.toFloat() }
        return DaqcValue.Quantity.of(sum / data.size, Units.VOLT)
    }

    fun median(): Pair<Instant, DaqcValue.Quantity<ElectricPotential>?>{
        val data = dataMap.sortedByDescending {
            ((it.second ?: DaqcValue.Quantity.of(0,Units.VOLT)).quantity.value.toInt()) }
        return data[data.size/2]
    }
}