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

package org.tenkiv.kuantify.hardware.inputs

import arrow.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.definitions.channel.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import javax.measure.*
import javax.measure.quantity.*

/**
 * Abstract class for an input which takes frequency data from a single digital input.
 *
 * @param digitalInput The digital input
 */
abstract class ScDigitalFrequencySensor<Q : Quantity<Q>>(val digitalInput: DigitalInput) :
    QuantityInput<Q> {

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()
    final override val updateBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    final override val failureBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    override val isTransceiving get() = digitalInput.isTransceivingFrequency

    override val updateRate get() = digitalInput.updateRate

    init {
        launch {
            digitalInput.transitionFrequencyBroadcaster.consumeEach { measurement ->
                val convertedInput = convertInput(measurement.value)

                when (convertedInput) {
                    is Success -> _broadcastChannel.send(convertedInput.value at measurement.instant)
                    is Failure -> _failureBroadcastChannel.send(convertedInput.exception at measurement.instant)
                }
            }
        }
    }

    @Volatile
    var averageTransitionFrequency: DaqcQuantity<Frequency> = 2.hertz.toDaqc()

    override fun startSampling() = digitalInput.startSamplingTransitionFrequency(averageTransitionFrequency)

    override fun stopTransceiving() = digitalInput.stopTransceiving()

    /**
     * Function to convert the [Frequency] of the digital input to a [DaqcQuantity] or return an error.
     *
     * @param frequency The [Frequency] measured by the digital input.
     * @return A [Try] of either a [DaqcQuantity] or an error.
     */
    protected abstract fun convertInput(frequency: ComparableQuantity<Frequency>): Try<DaqcQuantity<Q>>
}