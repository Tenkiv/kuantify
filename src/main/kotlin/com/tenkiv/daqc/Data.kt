package com.tenkiv.daqc

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.hardware.definitions.channel.Output
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Time


/**
 * Created by tenkiv on 3/20/17.
 */

data class ArffDataSetStub(val data: Int)

data class TriggerCondition<T : DaqcValue>(val input: Input<T>, val condition: (T) -> Boolean) {
    var lastValue: T? = null
    var hasBeenReached: Boolean = false
}

interface UpdatableListener<T> {

    suspend fun onUpdate(updatable: Updatable<T>, value: T)

}


sealed class DaqcValue

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
    PULSE_WIDTH_MODULATE,

}

enum class AnalogAccuracy {
    ONE_TENTH_VOLT,
    ONE_HUNDREDTH_VOLT,
    ONE_THOUSANDTH_VOLT,
    ONE_TEN_THOUSANDTH_VOLT,
    ONE_HUNDRED_THOUSANDTH_VOLT,
    ONE_MILLIONTH_VOLT
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
        try {
            val r = super.add(element)
            if (size > maxSize) {
                removeRange(0, size - maxSize)
            }
            return r
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun youngest(): T = get(size - 1)

    fun oldest(): T = get(0)
}

suspend fun <T : DaqcValue> BroadcastChannel<T>.consumeAndReturn(action: suspend (T) -> kotlin.Unit): SubscriptionReceiveChannel<T> {
    val channel = open()
    launch(DAQC_CONTEXT) { channel.use { channel -> for (x in channel) action(x) } }
    return channel
}


