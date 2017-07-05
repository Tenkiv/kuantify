package com.tenkiv.daqc

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.hardware.definitions.channel.Output
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.physikal.core.asType
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency
import javax.measure.quantity.Time

data class ArffDataSetStub(val data: Int)

data class TriggerCondition<T : ValueInstant<DaqcValue>>(val input: Input<T>, val condition: (T) -> Boolean) {
    var lastValue: T? = null
    var hasBeenReached: Boolean = false
}

interface UpdatableListener<T> {

    suspend fun onUpdate(updatable: Updatable<T>, value: T)

}

sealed class DaqcValue {
    companion object {

        inline fun binaryFromString(input: String): DaqcValue? {
            if (input == BinaryState.On.toString()) {
                return BinaryState.On
            }
            if (input == BinaryState.On.toString()) {
                return BinaryState.Off
            }
            throw Exception("Placeholder Exception")
        }

        inline fun <reified Q : Quantity<Q>> quantityFromString(input: String): DaqcQuantity<Q> {
            val quant: ComparableQuantity<Q>? = Quantities.getQuantity(input).asType<Q>()
            if (quant != null) {
                return DaqcQuantity.of(quant)
            } else {
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


enum class OutputCommand {
    SET_VALUE,
    DELAY,
    PULSE_WIDTH_MODULATE
}

sealed class LineNoiseFrequency {

    object Ignore : LineNoiseFrequency()

    data class AccountFor(val frequency: ComparableQuantity<Frequency>) : LineNoiseFrequency()

}

abstract class ControllerCommand {
    abstract val outputCommand: OutputCommand
}

data class PWMOutputCommand(val output: Output<BinaryState>, val pwmDutyCycle: Float) : ControllerCommand() {
    override val outputCommand: OutputCommand = OutputCommand.PULSE_WIDTH_MODULATE
}

data class SetAnalogOutputCommand(val output: Output<DaqcQuantity<ElectricPotential>>,
                                  val volts: Quantity<ElectricPotential>) : ControllerCommand() {
    override val outputCommand: OutputCommand = OutputCommand.SET_VALUE
}

data class SetDigitalOutputCommand(val output: Output<BinaryState>, val state: Boolean) : ControllerCommand() {
    override val outputCommand: OutputCommand = OutputCommand.SET_VALUE
}

data class DelayCommand(val delay: Quantity<Time>) : ControllerCommand() {
    override val outputCommand: OutputCommand = OutputCommand.DELAY
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

suspend fun <T : ValueInstant<DaqcValue>> BroadcastChannel<T>.consumeAndReturn(action: suspend (T) -> kotlin.Unit): SubscriptionReceiveChannel<T> {
    val channel = open()
    channel.use { channel -> for (x in channel) action(x) }
    return channel
}


