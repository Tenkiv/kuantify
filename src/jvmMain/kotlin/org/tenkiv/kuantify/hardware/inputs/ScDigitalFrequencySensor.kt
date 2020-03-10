/*
 * Copyright 2020 Tenkiv, Inc.
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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import physikal.*

/**
 * Abstract class for an input which takes frequency data from a single digital input.
 *
 * @param digitalInput The digital input
 */
public abstract class ScDigitalFrequencySensor<QT : Quantity<QT>>(val digitalInput: DigitalInput<*>) :
    QuantityInput<QT> {

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<QT>>()
    public final override val updateBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<QT>>
        get() = _broadcastChannel

    private val _transformErrorBroadcaster = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    public val transformErrorBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _transformErrorBroadcaster

    public final override val isTransceiving get() = digitalInput.isTransceivingFrequency

    public final override val updateRate get() = digitalInput.updateRate

    public val avgFrequency: UpdatableQuantity<Frequency> get() = digitalInput.avgFrequency

    init {
        launch {
            digitalInput.transitionFrequencyBroadcaster.consumeEach { measurement ->

                when (val convertedInput = transformInput(measurement.value)) {
                    is Result.Success -> _broadcastChannel.send(convertedInput.value at measurement.instant)
                    is Result.Failure -> {
                        _transformErrorBroadcaster.send(convertedInput.error at measurement.instant)
                    }
                }
            }
        }
    }

    public final override fun startSampling() {
        digitalInput.startSamplingTransitionFrequency()
    }

    public final override fun stopTransceiving() {
        digitalInput.stopTransceiving()
    }

    /**
     * Function to convert the [Frequency] of the digital input to a [DaqcQuantity] or return an error.
     *
     * @param frequency The [Frequency] measured by the digital input.
     * @return A [Result] of either a [DaqcQuantity] or an error.
     */
    protected abstract fun transformInput(frequency: Quantity<Frequency>): Result<DaqcQuantity<QT>, Throwable>
}