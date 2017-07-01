package com.tenkiv.daqc

import com.tenkiv.DAQC_CONTEXT
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant

class Trigger<T : DaqcValue>(vararg triggerConditions: TriggerCondition<T>,
                             val triggerOnSimultaneousValues: Boolean = false,
                             val triggerOnce: Boolean = true,
                             triggerFunction: () -> Unit) {

    private val channelList: MutableList<SubscriptionReceiveChannel<ValueInstant<T>>> = ArrayList()

    fun stop() { if (triggerOnce) { channelList.forEach { it.close() } } }

    init {
        launch(DAQC_CONTEXT) {
            triggerConditions.forEach {
                channelList.add(it.input.broadcastChannel.consumeAndReturn({ update ->

                    val currentVal = update

                    it.lastValue = currentVal.value

                    if (it.condition(currentVal.value)) {
                        it.hasBeenReached = true

                        if (triggerOnSimultaneousValues) {
                            triggerConditions.all {
                                val value = it.lastValue ?: return@all false
                                it.condition(value)
                            }.apply { triggerFunction.invoke() }

                            stop()

                        } else {
                            triggerConditions.all { it.hasBeenReached }.apply { triggerFunction.invoke() }

                            stop()
                        }
                    }
                }))
            }
        }
    }
}