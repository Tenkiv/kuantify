package com.tenkiv.daqc

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.tekdaqc.hardware.AAnalogInput
import tec.uom.se.ComparableQuantity

/**
 * Created by tenkiv on 4/4/17.
 */
class Trigger(vararg triggerConditions: TriggerCondition<DaqcValue>, val triggerOnSimultaneousValues: Boolean = false, triggerFunction: () -> Unit){

    //val typeTrigger = triggerConditions as? Array<TriggerCondition<Any>> ?: throw Exception()

    init{

        triggerConditions.forEach {

            val triggerProc = object : UpdatableListener<DaqcValue> {

                override fun onUpdate(data: Updatable<DaqcValue>) {

                    val currentVal = data.value

                    it.lastValue = currentVal

                    if (currentVal != null && it.condition(currentVal)) {
                        it.hasBeenReached = true

                        if (triggerOnSimultaneousValues) {
                            triggerConditions.all { it.condition(it?.lastValue) }.apply { triggerFunction.invoke() }

                            triggerConditions.forEach { it.input.listeners.remove(this) }

                        } else {
                            triggerConditions.all { it.hasBeenReached }.apply { triggerFunction.invoke() }

                            triggerConditions.forEach { it.input.listeners.remove(this) }
                        }
                    }
                }
            }

            it.input.listeners.add(triggerProc)
        }
    }
}