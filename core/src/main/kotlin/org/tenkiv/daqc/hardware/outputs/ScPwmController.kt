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

package org.tenkiv.daqc.hardware.outputs

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.now
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.QuantityMeasurement
import org.tenkiv.daqc.QuantityOutput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import javax.measure.Quantity
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

/**
 * Abstract class for a controller which outputs a percent to a single digital output channel from a [Quantity].
 *
 * @param digitalOutput The digital output
 */
abstract class ScPwmController<Q : Quantity<Q>>(val digitalOutput: DigitalOutput) :
    QuantityOutput<Q> {

    override val isActive get() = digitalOutput.isActiveForPwm

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()

    final override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    override fun setOutput(setting: DaqcQuantity<Q>) {
        digitalOutput.pulseWidthModulate(convertOutput(setting))
        //TODO Change this to broadcast new setting when the board confirms the setting was received.
        _broadcastChannel.offer(setting.now())
    }

    /**
     * Converts a [DaqcQuantity] to a usable percent for a pwm digital output.
     *
     * @param setting The [DaqcQuantity] to be converted into a pwm percentage.
     * @return The value converted into a pwm percentage.
     */
    protected abstract fun convertOutput(setting: DaqcQuantity<Q>): DaqcQuantity<Dimensionless>

    override fun deactivate() = digitalOutput.deactivate()
}