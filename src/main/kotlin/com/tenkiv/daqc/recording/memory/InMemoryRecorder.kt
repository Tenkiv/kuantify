package com.tenkiv.daqc.recording.memory

import com.tenkiv.daqc.BoundedFirstInFirstOutArrayList
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.recording.Recorder
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import java.time.Instant
import java.util.*

/**
 * Created by tenkiv on 5/15/17.
 */
abstract class InMemoryRecorder<T: DaqcValue>(val updatable: Updatable<T>, val maximumSize: Int = 10000, name: String = "") : Recorder<T>(null, mapOf(Pair(updatable, name))) {

    private val _dataMap: MutableList<Pair<Instant, T?>> = Collections.synchronizedList(BoundedFirstInFirstOutArrayList(maximumSize))

    val dataMap: List<Pair<Instant, T?>>
        get() = ArrayList(_dataMap)

    override val broadcastChannel = ConflatedBroadcastChannel<DaqcValue.Boolean>()

    suspend override fun onUpdate(updatable: Updatable<T>, value: T) {
        _dataMap.add(Pair(Instant.now(),value))
    }
}