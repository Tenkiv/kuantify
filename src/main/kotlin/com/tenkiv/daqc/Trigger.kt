package com.tenkiv.daqc

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.tekdaqc.hardware.AAnalogInput
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units

/**
 * Created by tenkiv on 4/4/17.
 */
class Trigger<T: DaqcValue>(vararg triggerConditions: TriggerCondition<T>,
                            val triggerOnSimultaneousValues: Boolean = false,
                            val triggerOnce: Boolean = true,
                            triggerFunction: () -> Unit){

    val channelList: MutableList<SubscriptionReceiveChannel<Updatable<T>>> = ArrayList()

    init{

        fun removeTriggerConditionListeners(){ if(triggerOnce) { channelList.forEach{ it.close() } } }

        launch(CommonPool){

            triggerConditions.forEach {
                channelList.add(it.input.broadcastChannel.consumeAndReturn({ update ->

                    val currentVal = update.value

                    it.lastValue = currentVal

                    if (currentVal != null && it.condition(currentVal)) {
                        it.hasBeenReached = true

                        if (triggerOnSimultaneousValues) {
                            triggerConditions.all {
                                val value = it.lastValue ?: return@all false
                                it.condition(value)
                            }.apply { triggerFunction.invoke() }

                            removeTriggerConditionListeners()

                        } else {
                            triggerConditions.all { it.hasBeenReached }.apply { triggerFunction.invoke() }

                            removeTriggerConditionListeners()
                        }
                    }
                }))
            }
        }
    }
}