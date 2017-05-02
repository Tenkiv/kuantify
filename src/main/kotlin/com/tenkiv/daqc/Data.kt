package com.tenkiv.daqc

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import javax.measure.Unit


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

    class Quantity<Q : javax.measure.Quantity<Q>> private constructor(private val quantity: ComparableQuantity<Q>)
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

