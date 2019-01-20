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

import kotlinx.coroutines.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.definitions.channel.*
import javax.measure.quantity.*

/**
 * A simple simple implementation of a binary frequency sensor
 *
 * @param digitalInput The [DigitalInput] that is being read from.
 */
class SimpleDigitalFrequencySensor internal constructor(val digitalInput: DigitalInput) :
    QuantityInput<Frequency>, CoroutineScope by digitalInput {

    override val updateBroadcaster get() = digitalInput.transitionFrequencyBroadcaster

    override val failureBroadcaster get() = digitalInput.failureBroadcaster

    override val isTransceiving get() = digitalInput.isTransceivingFrequency

    override val updateRate get() = digitalInput.updateRate

    override fun startSampling() = digitalInput.startSamplingTransitionFrequency()

    override fun stopTransceiving() = digitalInput.stopTransceiving()

}