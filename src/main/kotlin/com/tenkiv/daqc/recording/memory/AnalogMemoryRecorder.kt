package com.tenkiv.daqc.recording.memory

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import tec.uom.se.unit.Units
import java.time.Instant
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 5/22/17.
 */
class AnalogMemoryRecorder(updatable: Updatable<DaqcValue.Quantity<ElectricPotential>>,
                           maximumSize: Int,
                           name: String): InMemoryRecorder<DaqcValue.Quantity<ElectricPotential>>(updatable, maximumSize, name) {

    fun average(): DaqcValue.Quantity<ElectricPotential> {
        var sum = 0.0
        val data = dataMap
        data.forEach {
            sum += (it.second ?: DaqcValue.Quantity.of(0, Units.VOLT)).to(Units.VOLT).value.toFloat() }
        return DaqcValue.Quantity.of(sum / data.size, Units.VOLT)
    }

    fun median(): Pair<Instant, DaqcValue.Quantity<ElectricPotential>?>{
        val data = dataMap.sortedByDescending {
            ((it.second ?: DaqcValue.Quantity.of(0, Units.VOLT)).quantity.value.toInt()) }
        return data[data.size/2]
    }


}