package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.coral.ValueInstant

interface Input<T : ValueInstant<DaqcValue>> : Updatable<T> {

    suspend fun processNewMeasurement(measurement: T) = broadcastChannel.send(measurement)

    fun activate()

    fun deactivate()

}