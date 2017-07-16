package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.*
import org.tenkiv.daqc.hardware.definitions.Updatable

typealias QuantityInput<Q> = Input<DaqcQuantity<Q>>
typealias BinaryStateInput = Input<BinaryState>

interface Input<T : DaqcValue> : Updatable<ValueInstant<T>> {

    suspend fun sendNewMeasurement(measurement: ValueInstant<T>) = broadcastChannel.send(measurement)

    val isActive: Boolean

    fun activate()

    fun deactivate()

    fun addTrigger(condition: (ValueInstant<T>) -> Boolean, onTrigger: () -> Unit): Trigger<T> {
        return Trigger(TriggerCondition(this@Input, condition), triggerFunction = onTrigger)
    }
}