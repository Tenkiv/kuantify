package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import org.tenkiv.coral.ValueInstant

class Trigger<T : DaqcValue>(vararg triggerConditions: TriggerCondition<T>,
                             val triggerOnSimultaneousValues: Boolean = false,
                             val triggerOnce: Boolean = true,
                             triggerFunction: () -> Unit) {

    private val channelList: MutableList<SubscriptionReceiveChannel<ValueInstant<T>>> = ArrayList()

    fun stop() {
        if (triggerOnce) {
            channelList.forEach { it.close() }
        }
    }

    init {
        triggerConditions.forEach {
            channelList.add(it.input.broadcastChannel.consumeAndReturn({ update ->
                val currentVal = update

                it.lastValue = currentVal

                if (it.condition(currentVal)) {
                    it.hasBeenReached = true

                    if (triggerOnSimultaneousValues) {
                        stop()
                        triggerConditions.all {
                            val value = it.lastValue ?: return@all false
                            it.condition(value)
                        }.apply { triggerFunction.invoke() }


                    } else {
                        stop()
                        triggerConditions.all { it.hasBeenReached }.apply { triggerFunction.invoke() }
                    }
                }
            }))
        }
    }
}