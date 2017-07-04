package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.Trigger
import com.tenkiv.daqc.TriggerCondition
import com.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.coral.ValueInstant

interface Input<T : ValueInstant<DaqcValue>> : Updatable<T> {

    suspend fun processNewMeasurement(measurement: T) = broadcastChannel.send(measurement)

    val isActivated: Boolean

    fun activate()

    fun deactivate()

    fun addTrigger(condition: (T) -> Boolean, onTrigger: () -> Unit): Trigger<T>{
        return Trigger(TriggerCondition(this@Input,condition), triggerFunction = onTrigger)
    }
}