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

package org.tenkiv.kuantify

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

fun <T : DaqcValue> CoroutineScope.Trigger(
    triggerOnSimultaneousValues: Boolean = false,
    maxTriggerCount: MaxTriggerCount = MaxTriggerCount.Limited(1),
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
): Trigger<T> = Trigger(
    scope = this,
    triggerOnSimultaneousValues = triggerOnSimultaneousValues,
    maxTriggerCount = maxTriggerCount,
    triggerConditions = *triggerConditions,
    triggerFunction = triggerFunction
)

fun <T : DaqcValue> CoroutineScope.Trigger(
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
): Trigger<T> = Trigger(
    this,
    false,
    MaxTriggerCount.Limited(1),
    *triggerConditions,
    triggerFunction = triggerFunction
)

fun <T : DaqcValue> CoroutineScope.Trigger(
    maxTriggerCount: MaxTriggerCount,
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
): Trigger<T> = Trigger(
    this,
    false,
    maxTriggerCount,
    *triggerConditions,
    triggerFunction = triggerFunction
)

fun <T : DaqcValue> CoroutineScope.Trigger(
    triggerOnSimultaneousValues: Boolean,
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
): Trigger<T> = Trigger(
    this,
    triggerOnSimultaneousValues,
    MaxTriggerCount.Limited(1),
    *triggerConditions,
    triggerFunction = triggerFunction
)

/**
 * Class which acts as a monitor on an Input to execute a command when a certain state is met.
 *
 * @param triggerOnSimultaneousValues If the Trigger should fire only when all values are met at the same time.
 * @param maxTriggerCount The [MaxTriggerCount] for how many times the trigger should fire until it terminates.
 * @param triggerConditions The [TriggerCondition]s which need to be met for a trigger to fire.
 * @param triggerFunction The function to be executed when the trigger fires.
 */
class Trigger<T : DaqcValue> internal constructor(
    scope: CoroutineScope,
    val triggerOnSimultaneousValues: Boolean = false,
    val maxTriggerCount: MaxTriggerCount = MaxTriggerCount.Limited(1),
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
) : CoroutineScope {
    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    private val channelList: MutableList<ReceiveChannel<ValueInstant<T>>> = ArrayList()

    /**
     * Stops the [Trigger] and cancels the open channels.
     */
    fun cancel() {
        if (maxTriggerCount is MaxTriggerCount.Limited && maxTriggerCount.atomicCount.get() > 0) {
            maxTriggerCount.atomicCount.decrementAndGet()

            if (maxTriggerCount.atomicCount.get() <= 0) {
                channelList.forEach { it.cancel() }
            }
        }
        job.cancel()
    }

    init {
        if (!(maxTriggerCount is MaxTriggerCount.Limited && maxTriggerCount.atomicCount.get() == 0)) {
            triggerConditions.forEach {
                channelList.add(it.input.updateBroadcaster.consumeAndReturn(this) { update ->
                    val currentVal = update

                    it.lastValue = currentVal

                    if (it.condition(currentVal)) {
                        it.hasBeenReached = true

                        if (triggerOnSimultaneousValues) {
                            cancel()
                            triggerConditions.all {
                                val value = it.lastValue ?: return@all false
                                it.condition(value)
                            }.apply { triggerFunction() }

                        } else {
                            cancel()
                            triggerConditions.all { it.hasBeenReached }.apply { triggerFunction() }
                        }
                    }
                })
            }
        }
    }
}

/**
 * Sealed class to determine the number of times a [Trigger] should fire.
 */
sealed class MaxTriggerCount {

    /**
     * Class which sets the number of times a [Trigger] can fire.
     */
    data class Limited(val totalCount: Int) : MaxTriggerCount() {

        /**
         * The number of charges left in the [Trigger]
         */
        val remainingCount get() = atomicCount.get()

        internal val atomicCount = AtomicInteger(totalCount)

    }

    /**
     * Class which sets a [Trigger] to fire unlimited times.
     */
    object Unlimited : MaxTriggerCount()
}

//TODO: Should support IOStrand, not just Input
/**
 * The condition upon which the [Trigger] will fire.
 *
 * @param input The [Input] to monitor.
 * @param condition The conditions upon which to execute the [Trigger]'s function.
 */
data class TriggerCondition<T : DaqcValue>(val input: Input<T>, val condition: (ValueInstant<T>) -> Boolean) {
    var lastValue: ValueInstant<T>? = null
    var hasBeenReached: Boolean = false
}