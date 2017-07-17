package org.tenkiv.daqc

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.channel.Input
import org.tenkiv.physikal.core.asType
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import java.time.Instant
import java.util.zip.DataFormatException
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Frequency
import kotlin.coroutines.experimental.CoroutineContext

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

        fun binaryFromString(input: String): DaqcValue {
            if (input == BinaryState.On.toString()) {
                return BinaryState.On
            }
            if (input == BinaryState.Off.toString()) {
                return BinaryState.Off
            }
            throw DataFormatException("Data with BinaryState not found")
        }

        inline fun <reified Q : Quantity<Q>> quantityFromString(input: String): DaqcQuantity<Q> {
            val quant: ComparableQuantity<Q>? = Quantities.getQuantity(input).asType<Q>()
            if (quant != null) {
                return DaqcQuantity.of(quant)
            } else {
                throw DataFormatException("Data with Quantity value not found")
            }
        }
    }
}

sealed class BinaryState : DaqcValue() {

    object On : BinaryState() {
        override fun toString() = "BinaryState.ON"
    }

    object Off : BinaryState() {
        override fun toString() = "BinaryState.OFF"

    }

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

fun <T : ValueInstant<DaqcValue>> BroadcastChannel<T>.consumeAndReturn(context: CoroutineContext = CommonPool,
                                                                       action: suspend (T) -> kotlin.Unit):
        SubscriptionReceiveChannel<T> {
    val subChannel = open()
    launch(context) {
        subChannel.use { channel -> for (x in channel) action(x) }
    }
    return subChannel
}

enum class DigitalStatus {
    ACTIVATED_STATE,
    ACTIVATED_FREQUENCY,
    ACTIVATED_PWM,
    DEACTIVATED
}

data class StoredData(val time: Long, val value: String) {
    fun <T> getValueInstant(deserializer: (String) -> T): ValueInstant<T>
            = try {
        deserializer(value).at(Instant.ofEpochMilli(time))
    } catch (e: Exception) {
        throw e
    }
}
