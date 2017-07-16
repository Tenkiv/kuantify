package org.tenkiv.daqc

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.channel.Input
import org.tenkiv.physikal.core.asType
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Frequency

data class ArffDataSetStub(val data: Int)

data class TriggerCondition<T : DaqcValue>(val input: Input<T>, val condition: (ValueInstant<T>) -> Boolean) {
    var lastValue: ValueInstant<T>? = null
    var hasBeenReached: Boolean = false
}

interface UpdatableListener<T> {

    suspend fun onUpdate(updatable: Updatable<T>, value: T)

}

sealed class DaqcValue {
    companion object {

        fun binaryFromString(input: String): DaqcValue? {
            if (input == BinaryState.On.toString()) {
                return BinaryState.On
            }
            if (input == BinaryState.Off.toString()) {
                return BinaryState.Off
            }
            throw Exception("Placeholder Exception")
        }

        inline fun <reified Q : Quantity<Q>> quantityFromString(input: String): DaqcQuantity<Q> {
            val quant: ComparableQuantity<Q>? = Quantities.getQuantity(input).asType<Q>()
            if (quant != null) {
                return DaqcQuantity.of(quant)
            } else {
                //TODO: Placeholder exception
                throw Exception("Placeholder Exception")
            }
        }
    }
}

sealed class BinaryState : DaqcValue() {

    object On : BinaryState()

    object Off : BinaryState()

}

data class DaqcQuantity<Q : Quantity<Q>>(private val quantity: ComparableQuantity<Q>)
    : DaqcValue(), ComparableQuantity<Q> by quantity {

    override fun toString() = quantity.toString()

    companion object {
        fun <Q : Quantity<Q>> of(value: Number, unit: Unit<Q>) =
                DaqcQuantity(Quantities.getQuantity(value, unit))

        fun <Q : Quantity<Q>> of(quantity: ComparableQuantity<Q>) =
                DaqcQuantity(quantity)

        fun <Q : Quantity<Q>> of(instant: ValueInstant<ComparableQuantity<Q>>) =
                DaqcQuantity(instant.value)
    }
}


sealed class LineNoiseFrequency {

    data class AccountFor(val frequency: ComparableQuantity<Frequency>) : LineNoiseFrequency()

    object Ignore : LineNoiseFrequency()

}

class BoundedFirstInFirstOutArrayList<T>(val maxSize: Int) : ArrayList<T>() {

    override fun add(element: T): Boolean {
        val r = super.add(element)
        if (size > maxSize) {
            removeRange(0, size - maxSize)
        }
        return r
    }

    fun youngest(): T = get(size - 1)

    fun oldest(): T = get(0)
}

//TODO: Clean this up
fun <T : ValueInstant<DaqcValue>> BroadcastChannel<T>.consumeAndReturn(action: suspend (T) -> kotlin.Unit):
        SubscriptionReceiveChannel<T> {
    val channel = open()
    launch(CommonPool) {
        channel.use { channel -> for (x in channel) action(x) }
    }
    return channel
}

enum class DigitalStatus {
    ACTIVATED_STATE,
    ACTIVATED_FREQUENCY,
    ACTIVATED_PWM,
    DEACTIVATED
}
