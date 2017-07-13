package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.Trigger
import org.tenkiv.daqc.TriggerCondition
import org.tenkiv.daqc.hardware.definitions.Updatable

interface Input<T : ValueInstant<DaqcValue>> : Updatable<T> {

    suspend fun sendNewMeasurement(measurement: T) = broadcastChannel.send(measurement)

    val isActive: Boolean

    fun activate()

    fun deactivate()

    fun addTrigger(condition: (T) -> Boolean, onTrigger: () -> Unit): Trigger<T> {
        return Trigger(TriggerCondition(this@Input, condition), triggerFunction = onTrigger)
    }
}