/*
 * Copyright 2018 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.lib.consumeAndReturn
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

    private val channelList: MutableList<ReceiveChannel<ValueInstant<T>>> = ArrayList()

    fun stop() {
        if (maxTriggerCount is MaxTriggerCount.Limited && maxTriggerCount.atomicCount.get() > 0) {
            maxTriggerCount.atomicCount.decrementAndGet()

            if (maxTriggerCount.atomicCount.get() <= 0) {
                channelList.forEach { it.cancel() }
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

//TODO: Should support IOChannel, not just Input
data class TriggerCondition<T : DaqcValue>(val input: Input<T>, val condition: (ValueInstant<T>) -> Boolean) {
    var lastValue: ValueInstant<T>? = null
    var hasBeenReached: Boolean = false
}