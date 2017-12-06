package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import org.tenkiv.coral.ValueInstant

class Trigger<T : DaqcValue>(val triggerOnSimultaneousValues: Boolean = false,
                             val maxTimesTriggered: MaxTriggerCount = TriggerCount(1),
                             vararg triggerConditions: TriggerCondition<T>,
                             triggerFunction: () -> Unit) {

    constructor(vararg triggerConditions: TriggerCondition<T>,
                triggerFunction: () -> Unit):
            this(false,
                    TriggerCount(1),
                    *triggerConditions,
                    triggerFunction = triggerFunction)

    constructor(maxTimesTriggered: MaxTriggerCount,
                vararg triggerConditions: TriggerCondition<T>,
                triggerFunction: () -> Unit):
            this(false,
                    maxTimesTriggered,
                    *triggerConditions,
                    triggerFunction = triggerFunction)

    constructor(triggerOnSimultaneousValues: Boolean,
                vararg triggerConditions: TriggerCondition<T>,
                triggerFunction: () -> Unit):
            this(triggerOnSimultaneousValues,
                    TriggerCount(1),
                    *triggerConditions,
                    triggerFunction = triggerFunction)

    private val channelList: MutableList<SubscriptionReceiveChannel<ValueInstant<T>>> = ArrayList()

    fun stop() {
        if (maxTimesTriggered is TriggerCount && maxTimesTriggered.count > 0) {
            maxTimesTriggered.count--
        }
        if (maxTimesTriggered is TriggerCount && maxTimesTriggered.count <= 0){
            channelList.forEach { it.close() }
        }
    }

    init {
        if (!(maxTimesTriggered is TriggerCount && maxTimesTriggered.count == 0)) {
            triggerConditions.forEach {
                channelList.add(it.input.broadcastChannel.consumeAndReturn { update ->
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
                })
            }
        }
    }
}

sealed class MaxTriggerCount

data class TriggerCount(@Volatile var count: Int): MaxTriggerCount()

class UnlimitedCount: MaxTriggerCount()