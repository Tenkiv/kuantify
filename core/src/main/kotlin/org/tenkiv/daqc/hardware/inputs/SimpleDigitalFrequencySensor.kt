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

package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.daqc.data.DaqcQuantity
import org.tenkiv.daqc.data.toDaqc
import org.tenkiv.daqc.gate.receive.input.QuantityInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.physikal.core.hertz
import javax.measure.quantity.Frequency

class SimpleDigitalFrequencySensor internal constructor(val digitalInput: DigitalInput) :
    QuantityInput<Frequency> {

    @Volatile
    var avgFrequency: DaqcQuantity<Frequency> = 1.hertz.toDaqc()

    override val broadcastChannel get() = digitalInput.transitionFrequencyBroadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForTransitionFrequency

    override val updateRate get() = digitalInput.updateRate

    override fun activate() = digitalInput.activateForTransitionFrequency(avgFrequency)

    override fun deactivate() = digitalInput.deactivate()

}