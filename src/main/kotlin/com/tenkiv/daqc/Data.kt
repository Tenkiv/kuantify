package com.tenkiv.daqc

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.hardware.definitions.channel.Output
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import tec.uom.se.unit.Units
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Time


/**
 * Created by tenkiv on 3/20/17.
 */

data class ArffDataSetStub(val data: Int)

data class TriggerCondition<T: DaqcValue>(val input: Input<T>, val condition: (T) -> Boolean){
    var lastValue: T? = null
    var hasBeenReached: Boolean = false
}

interface UpdatableListener<T: DaqcValue>{ fun onUpdate(updatedObject: Updatable<T>) }


sealed class DaqcValue {

    data class Boolean(val isOn: kotlin.Boolean) : DaqcValue()

    // Quantity was private before. Why?
    class Quantity<Q : javax.measure.Quantity<Q>> private constructor(val quantity: ComparableQuantity<Q>)
        : DaqcValue(), ComparableQuantity<Q> by quantity {

        override fun equals(other: Any?): kotlin.Boolean {
            return quantity == other
        }

        override fun hashCode(): Int {
            return quantity.hashCode()
        }

        override fun toString(): String {
            return quantity.toString()
        }

        companion object {
            fun <Q : javax.measure.Quantity<Q>> of(value: Number, unit: Unit<Q>) =
                    Quantity(Quantities.getQuantity(value, unit))
        }
    }
}

enum class OutputCommand{
    SET_VALUE,
    DELAY,
    PULSE_WIDTH_MODULATE,

}

abstract class ControllerCommand{
    abstract val outputCommand: OutputCommand
}

data class PWMOutputCommand(val output: Output<DaqcValue.Boolean>, val pwmDutyCycle: Float): ControllerCommand(){
    override val outputCommand: OutputCommand  = OutputCommand.PULSE_WIDTH_MODULATE
}

data class SetAnalogOutputCommand(val output: Output<DaqcValue.Quantity<ElectricPotential>>, val volts: Quantity<ElectricPotential>): ControllerCommand(){
    override val outputCommand: OutputCommand = OutputCommand.SET_VALUE
}

data class SetDigitalOutputCommand(val output: Output<DaqcValue.Boolean>, val state: Boolean): ControllerCommand(){
    override val outputCommand: OutputCommand = OutputCommand.SET_VALUE
}

data class DelayCommand(val delay: Quantity<Time>): ControllerCommand(){
    override val outputCommand: OutputCommand = OutputCommand.DELAY
}


