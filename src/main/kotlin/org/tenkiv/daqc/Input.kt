package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import javax.measure.Quantity

typealias QuantityInput<Q> = Input<DaqcQuantity<Q>>

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

    fun addTrigger(condition: (ValueInstant<T>) -> Boolean, onTrigger: () -> Unit): Trigger<out T> =
        Trigger(
            triggerConditions = *arrayOf(TriggerCondition(this@Input, condition)),
            triggerFunction = onTrigger
        )

}

interface RangedInput<T> : Input<T> where T : DaqcValue, T : Comparable<T> {

    val possibleInputRange: ClosedRange<T>

}

interface BinaryStateInput : RangedInput<BinaryState> {

    override val possibleInputRange get() = BinaryState.range

}

// Kotlin compiler is getting confused about generics star projections if RangedInput (or a typealias) is used directly
// TODO: look into changing this to a typealias if generics compiler issue is fixed.
interface RangedQuantityInput<Q : Quantity<Q>> : RangedInput<DaqcQuantity<Q>>

class RangedQuantityInputBox<Q : Quantity<Q>>(
    input: QuantityInput<Q>,
    private val getPossibleInputRange: () -> ClosedRange<DaqcQuantity<Q>>
) : RangedQuantityInput<Q>, QuantityInput<Q> by input {

    override val possibleInputRange get() = getPossibleInputRange()

}

fun <Q : Quantity<Q>> QuantityInput<Q>.toNewRangedInput(possibleInputRange: () -> ClosedRange<DaqcQuantity<Q>>) =
    RangedQuantityInputBox(this, possibleInputRange)