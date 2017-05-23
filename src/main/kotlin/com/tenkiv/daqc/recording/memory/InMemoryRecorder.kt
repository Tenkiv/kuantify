package com.tenkiv.daqc.recording.memory

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.LimitedArrayList
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.recording.Recorder
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

    override val onDataUpdate = object: UpdatableListener<T> {
        override fun onUpdate(updatedObject: Updatable<T>) {
            _dataMap.add(Pair(Instant.now(),updatedObject.value))
        }
    }
}