package com.tenkiv.daqc.recording.memory

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import tec.uom.se.unit.Units
import java.time.Instant
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 5/22/17.
 */
class AnalogMemoryRecorder(updatable: Updatable<DaqcValue.DaqcQuantity<ElectricPotential>>,
                           maximumSize: Int,
                           name: String) : InMemoryRecorder<DaqcValue.DaqcQuantity<ElectricPotential>>(updatable, maximumSize, name) {

    fun average(): DaqcValue.DaqcQuantity<ElectricPotential> {
        var sum = 0.0
        val data = dataMap
        data.forEach {
            sum += (it.second ?: DaqcValue.DaqcQuantity.of(0, Units.VOLT)).to(Units.VOLT).value.toFloat()
        }
        return DaqcValue.DaqcQuantity.of(sum / data.size, Units.VOLT)
    }

    fun median(): Pair<Instant, DaqcValue.DaqcQuantity<ElectricPotential>?> {
        val data = dataMap.sortedByDescending {
            ((it.second ?: DaqcValue.DaqcQuantity.of(0, Units.VOLT)).value.toInt())
        }
        return data[data.size / 2]
    }


}