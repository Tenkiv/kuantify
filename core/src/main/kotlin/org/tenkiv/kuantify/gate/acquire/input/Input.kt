/*
 * Copyright 2019 Tenkiv, Inc.
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
 *
 */

package org.tenkiv.kuantify.gate.acquire.input

import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import javax.measure.*

typealias QuantityInput<Q> = Input<DaqcQuantity<Q>>

/**
 * Interface defining classes which act as inputs and measure or gather data.
 *
 * @param T The type of data given by this Input.
 */
interface Input<out T : DaqcValue> : IOStrand<T>, RatedTrackable<T> {

    /**
     * Exception is sent over this channel when something prevents the input from being received.
     */
    val failureBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>>

    /**
     * Activates the input alerting it to begin collecting and sending data.
     */
    fun startSampling()

}

/**
 * An Input whose type is both a [DaqcValue] and [Comparable] allowing it to be used in the default learning module
 * classes
 */
interface RangedInput<T> : Input<T>, RangedIOStrand<T> where T : DaqcValue, T : Comparable<T>

/**
 * A [RangedInput] which supports the [BinaryState] type.
 */
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