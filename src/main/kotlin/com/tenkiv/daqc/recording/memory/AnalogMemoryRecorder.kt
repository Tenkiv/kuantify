package com.tenkiv.daqc.recording.memory

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.definitions.Updatable
import tec.uom.se.unit.Units
import java.time.Instant
import javax.measure.quantity.ElectricPotential

class AnalogMemoryRecorder(updatable: Updatable<DaqcQuantity<ElectricPotential>>,
                           maximumSize: Int,
                           name: String) :
        InMemoryRecorder<DaqcQuantity<ElectricPotential>>(updatable, maximumSize, name) {

    fun average(): DaqcQuantity<ElectricPotential> {
        var sum = 0.0
        val data = dataMap
        data.forEach {
            sum += (it.second ?: DaqcQuantity.of(0, Units.VOLT)).to(Units.VOLT).value.toFloat()
        }
        return DaqcQuantity.of(sum / data.size, Units.VOLT)
    }

    fun median(): Pair<Instant, DaqcQuantity<ElectricPotential>?> {
        val data = dataMap.sortedByDescending {
            ((it.second ?: DaqcQuantity.of(0, Units.VOLT)).value.toInt())
        }
        return data[data.size / 2]
    }


}