package org.tenkiv.daqc

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.physikal.core.PhysicalUnit
import org.tenkiv.physikal.core.asType
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import java.time.Instant
import java.util.zip.DataFormatException
import javax.measure.Quantity
import javax.measure.quantity.Frequency
import kotlin.coroutines.experimental.CoroutineContext

data class ArffDataSetStub(val data: Int)

data class TriggerCondition<T : DaqcValue>(val input: Input<T>, val condition: (ValueInstant<T>) -> Boolean) {
    var lastValue: ValueInstant<T>? = null
    var hasBeenReached: Boolean = false
}

sealed class DaqcValue

sealed class BinaryState : DaqcValue(), Comparable<BinaryState> {

    object On : BinaryState() {

        override fun compareTo(other: BinaryState) =
            when (other) {
                is On -> 0
                is Off -> 1
            }

        override fun toString() = "BinaryState.ON"
    }

    object Off : BinaryState() {

        override fun compareTo(other: BinaryState) =
            when (other) {
                is On -> -1
                is Off -> 0
            }

        override fun toString() = "BinaryState.OFF"

    }

    companion object {

        val range = Off..On

        fun fromString(input: String): BinaryState {
            if (input == BinaryState.On.toString()) {
                return BinaryState.On
            }
            if (input == BinaryState.Off.toString()) {
                return BinaryState.Off
            }
            throw DataFormatException("Data with BinaryState not found")
        }
    }

}

class DaqcQuantity<Q : Quantity<Q>>(private val wrappedQuantity: ComparableQuantity<Q>) : DaqcValue(),
    ComparableQuantity<Q> by wrappedQuantity {

    override fun toString() = wrappedQuantity.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as DaqcQuantity<*>

        if (wrappedQuantity != other.wrappedQuantity) return false

        return true
    }

    override fun hashCode(): Int {
        return wrappedQuantity.hashCode()
    }


    companion object {
        fun <Q : Quantity<Q>> of(value: Number, unit: PhysicalUnit<Q>) =
            DaqcQuantity(Quantities.getQuantity(value, unit))

        fun <Q : Quantity<Q>> of(quantity: ComparableQuantity<Q>) =
            DaqcQuantity(quantity)

        fun <Q : Quantity<Q>> of(instant: QuantityMeasurement<Q>) =
            DaqcQuantity(instant.value)

        inline fun <reified Q : Quantity<Q>> fromString(input: String): DaqcQuantity<Q> {
            val quant: ComparableQuantity<Q>? = Quantities.getQuantity(input).asType()
            if (quant != null) {
                return DaqcQuantity.of(quant)
            } else {
                throw DataFormatException("Data with Quantity value not found")
            }
        }
    }
}


sealed class LineNoiseFrequency {

    data class AccountFor(val frequency: ComparableQuantity<Frequency>) : LineNoiseFrequency()

    object Ignore : LineNoiseFrequency()

}

fun <T : ValueInstant<DaqcValue>> BroadcastChannel<T>.consumeAndReturn(
    context: CoroutineContext = CommonPool,
    action: suspend (T) -> Unit
):
        SubscriptionReceiveChannel<T> {
    val subChannel = openSubscription()
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

data class PrimitiveValueInstant(val epochMilli: Long, val value: String) {

    inline fun <T> toValueInstant(deserializeValue: (String) -> T): ValueInstant<T> =
        deserializeValue(value) at Instant.ofEpochMilli(epochMilli)

}
