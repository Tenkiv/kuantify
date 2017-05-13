package com.tenkiv.daqc

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.tekdaqc.hardware.AAnalogInput
import tec.uom.se.ComparableQuantity

/**
 * Created by tenkiv on 4/4/17.
 */
class Trigger<T: DaqcValue>(vararg triggerConditions: TriggerCondition<T>,
                            val triggerOnSimultaneousValues: Boolean = false,
                            val triggerOnce: Boolean = true,
                            triggerFunction: () -> Unit){

    init{

        fun removeTriggerConditionListeners(listener: UpdatableListener<T>){
            if(triggerOnce) {
                triggerConditions.forEach { it.input.listeners.remove(listener) }
            }
        }

        triggerConditions.forEach {

            val triggerProc = object : UpdatableListener<T> {

                override fun onUpdate(updatedObject: Updatable<T>) {

                    val currentVal = updatedObject.value

                    it.lastValue = currentVal

                    if (currentVal != null && it.condition(currentVal)) {
                        it.hasBeenReached = true

                        if (triggerOnSimultaneousValues) {
                            triggerConditions.all {
                                val value = it.lastValue ?: return@all false
                                it.condition(value)
                            }.apply { triggerFunction.invoke() }

                            removeTriggerConditionListeners(this)

                        } else {
                            triggerConditions.all { it.hasBeenReached }.apply { triggerFunction.invoke() }

                            removeTriggerConditionListeners(this)
                        }
                    }
                }
            }

            it.input.listeners.add(triggerProc)
        }
    }
}