package org.tenkiv.daqc.hardware.definitions.channel

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.*
import org.tenkiv.daqc.hardware.definitions.Updatable

typealias QuantityInput<Q> = Input<DaqcQuantity<Q>>
typealias BinaryStateInput = Input<BinaryState>

/**
 * @param T The type of data given by this Input.
 */
interface Input<out T : DaqcValue> : Updatable<ValueInstant<T>> {

    val isActive: Boolean

    /**
     * Exception is sent over this channel when something prevents the input from being received.
     */
    val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>

    fun activate()

    fun deactivate()

    fun addTrigger(condition: (ValueInstant<T>) -> Boolean, onTrigger: () -> Unit): Trigger<out T> {
        return Trigger(TriggerCondition(this@Input, condition), triggerFunction = onTrigger)
    }
}