package com.tenkiv.daqc.recording.memory

import com.tenkiv.daqc.BinState
import com.tenkiv.daqc.hardware.definitions.Updatable
import java.time.Instant

/**
 * Created by tenkiv on 5/22/17.
 */
class DigitalMemoryRecorder(updatable: Updatable<BinState>,
                            maximumSize: Int,
                            name: String) : InMemoryRecorder<BinState>(updatable, maximumSize, name) {

    fun percentOn(): Double {
        var trueSum = 0
        val data = dataMap
        data.forEach {
            if (it.second == BinState.On) {
                trueSum++
            }
        }
        return (trueSum.toDouble() / data.size.toDouble())
    }

    fun median(): Pair<Instant, BinState?> {
        val data = dataMap.sortedByDescending {
            ((it.second == BinState.On))
        }
        return data[data.size / 2]
    }
}