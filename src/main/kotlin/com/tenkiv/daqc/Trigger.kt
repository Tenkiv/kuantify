package com.tenkiv.daqc

import com.tenkiv.DAQC_CONTEXT
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch

/**
 * Created by tenkiv on 4/4/17.
 */
class Trigger<T : DaqcValue>(vararg triggerConditions: TriggerCondition<T>,
                             val triggerOnSimultaneousValues: Boolean = false,
                             val triggerOnce: Boolean = true,
                             triggerFunction: () -> Unit) {

    val channelList: MutableList<SubscriptionReceiveChannel<T>> = ArrayList()

    init {

        fun removeTriggerConditionListeners() {
            if (triggerOnce) {
                channelList.forEach { it.close() }
            }
        }

        launch(DAQC_CONTEXT) {

            triggerConditions.forEach {
                channelList.add(it.input.broadcastChannel.consumeAndReturn({ update ->

                    val currentVal = update

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