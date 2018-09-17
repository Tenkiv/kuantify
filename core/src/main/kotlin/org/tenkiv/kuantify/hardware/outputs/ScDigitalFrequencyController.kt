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

package org.tenkiv.kuantify.hardware.outputs

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.now
import org.tenkiv.kuantify.QuantityMeasurement
import org.tenkiv.kuantify.data.DaqcQuantity
import org.tenkiv.kuantify.gate.control.output.QuantityOutput
import org.tenkiv.kuantify.gate.control.output.SettingResult
import org.tenkiv.kuantify.hardware.definitions.channel.DigitalOutput
import javax.measure.Quantity
import javax.measure.quantity.Frequency

/**
 * Abstract class for a controller which outputs a [Frequency] to a single digital output channel from a [Quantity].
 *
 * @param digitalOutput The digital output
 */
abstract class ScDigitalFrequencyController<Q : Quantity<Q>>(val digitalOutput: DigitalOutput) :
    QuantityOutput<Q> {

    override val isTransceiving get() = digitalOutput.isTransceivingFrequency

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()

    final override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    override fun setOutput(setting: DaqcQuantity<Q>, panicOnFailure: Boolean): SettingResult {
        val result = digitalOutput.sustainTransitionFrequency(convertOutput(setting), panicOnFailure)

        if (result is SettingResult.Success) _broadcastChannel.offer(setting.now())

        return result
    }

    /**
     * Converts a [DaqcQuantity] to a usable [Frequency] for a digital output.
     *
     * @param setting The [DaqcQuantity] to be converted into a [Frequency].
     * @return The value converted into a [Frequency].
     */
    protected abstract fun convertOutput(setting: DaqcQuantity<Q>): DaqcQuantity<Frequency>

    override fun stopTransceiving() = digitalOutput.stopTransceiving()
}