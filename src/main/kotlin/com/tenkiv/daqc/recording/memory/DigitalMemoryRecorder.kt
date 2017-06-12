package com.tenkiv.daqc.recording.memory

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.hardware.definitions.Updatable
import java.time.Instant

/**
 * Created by tenkiv on 5/22/17.
 */
class DigitalMemoryRecorder(updatable: Updatable<BinaryState>,
                            maximumSize: Int,
                            name: String) : InMemoryRecorder<BinaryState>(updatable, maximumSize, name) {

    fun percentOn(): Double {
        var trueSum = 0
        val data = dataMap
        data.forEach {
            if (it.second == BinaryState.On) {
                trueSum++
            }
        }
        return (trueSum.toDouble() / data.size.toDouble())
    }

    fun median(): Pair<Instant, BinaryState?> {
        val data = dataMap.sortedByDescending {
            ((it.second == BinaryState.On))
        }
        return data[data.size / 2]
    }
}