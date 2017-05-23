package com.tenkiv.daqc.recording.memory

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import tec.uom.se.unit.Units
import java.time.Instant
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 5/22/17.
 */
class DigitalMemoryRecorder(updatable: Updatable<DaqcValue.Boolean>,
                           maximumSize: Int,
                           name: String): InMemoryRecorder<DaqcValue.Boolean>(updatable, maximumSize, name) {

    fun percentOn(): Double {
        var trueSum = 0
        val data = dataMap
        data.forEach { if(it.second!!.isOn){trueSum++} }
        return (trueSum.toDouble()/data.size.toDouble())
    }

    fun median(): Pair<Instant, DaqcValue.Boolean?>{
        val data = dataMap.sortedByDescending {
            ((it.second?.isOn)) }
        return data[data.size/2]
    }
}