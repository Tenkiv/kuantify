package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import org.tenkiv.coral.ValueInstant
import java.util.concurrent.atomic.AtomicInteger

class Trigger<T : DaqcValue>(
    val triggerOnSimultaneousValues: Boolean = false,
    val maxTriggerCount: MaxTriggerCount = MaxTriggerCount.Limited(1),
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
) {

    constructor(
        vararg triggerConditions: TriggerCondition<T>,
        triggerFunction: () -> Unit
    ) :
            this(
                false,
                MaxTriggerCount.Limited(1),
                *triggerConditions,
                triggerFunction = triggerFunction
            )

    constructor(
        maxTimesTriggered: MaxTriggerCount,
        vararg triggerConditions: TriggerCondition<T>,
        triggerFunction: () -> Unit
    ) :
            this(
                false,
                maxTimesTriggered,
                *triggerConditions,
                triggerFunction = triggerFunction
            )

    constructor(
        triggerOnSimultaneousValues: Boolean,
        vararg triggerConditions: TriggerCondition<T>,
        triggerFunction: () -> Unit
    ) :
            this(
                triggerOnSimultaneousValues,
                MaxTriggerCount.Limited(1),
                *triggerConditions,
                triggerFunction = triggerFunction
            )

    private val channelList: MutableList<SubscriptionReceiveChannel<ValueInstant<T>>> = ArrayList()

    fun stop() {
        if (maxTriggerCount is MaxTriggerCount.Limited && maxTriggerCount.atomicCount.get() > 0) {
            maxTriggerCount.atomicCount.decrementAndGet()

            if (maxTriggerCount.atomicCount.get() <= 0) {
                channelList.forEach { it.close() }
            }
        }
    }

    init {
        if (!(maxTriggerCount is MaxTriggerCount.Limited && maxTriggerCount.atomicCount.get() == 0)) {
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
                            }.apply { triggerFunction() }

                        } else {
                            stop()
                            triggerConditions.all { it.hasBeenReached }.apply { triggerFunction() }
                        }
                    }
                })
            }
        }
    }
}

sealed class MaxTriggerCount {
    data class Limited(val totalCount: Int) : MaxTriggerCount() {

        val remainingCount get() = atomicCount.get()

        internal val atomicCount = AtomicInteger(totalCount)

    }

    object Unlimited : MaxTriggerCount()
}
