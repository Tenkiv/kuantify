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

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import tec.units.indriya.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Frequency

typealias QuantityInput<Q> = Input<DaqcQuantity<Q>>

/**
 * @param T The type of data given by this Input.
 */
interface Input<out T : DaqcValue> : Updatable<ValueInstant<T>> {

    val isActive: Boolean

    val sampleRate: ComparableQuantity<Frequency>

    /**
     * Exception is sent over this channel when something prevents the input from being received.
     */
    val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>

    fun activate()

    fun deactivate()

    fun addTrigger(condition: (ValueInstant<T>) -> Boolean, onTrigger: () -> Unit): Trigger<out T> =
        Trigger(
            triggerConditions = *arrayOf(TriggerCondition(this@Input, condition)),
            triggerFunction = onTrigger
        )

}

interface RangedInput<T> : Input<T>, RangedIO<T> where T : DaqcValue, T : Comparable<T>

interface BinaryStateInput : RangedInput<BinaryState> {

    override val valueRange get() = BinaryState.range

}

// Kotlin compiler is getting confused about generics star projections if RangedInput (or a typealias) is used directly
// TODO: look into changing this to a typealias if generics compiler issue is fixed.
interface RangedQuantityInput<Q : Quantity<Q>> : RangedInput<DaqcQuantity<Q>>

class RangedQuantityInputBox<Q : Quantity<Q>>(
    input: QuantityInput<Q>,
    override val valueRange: ClosedRange<DaqcQuantity<Q>>
) : RangedQuantityInput<Q>, QuantityInput<Q> by input

fun <Q : Quantity<Q>> QuantityInput<Q>.toNewRangedInput(valueRange: ClosedRange<DaqcQuantity<Q>>) =
    RangedQuantityInputBox(this, valueRange)