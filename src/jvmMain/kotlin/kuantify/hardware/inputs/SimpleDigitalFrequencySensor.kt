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

package kuantify.hardware.inputs

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kuantify.gate.acquire.*
import kuantify.gate.acquire.input.*
import kuantify.hardware.channel.*
import kuantify.lib.*
import kuantify.lib.physikal.*
import kuantify.trackable.*
import physikal.types.*

//TODO: inline
/**
 * A simple simple implementation of a binary frequency sensor
 *
 * @param digitalInput The [DigitalInput] that is being read from.
 */
internal class SimpleDigitalFrequencySensor(val digitalInput: DigitalInput) :
    QuantityInput<Frequency>, CoroutineScope by digitalInput {
    override val valueOrNull: QuantityMeasurement<Frequency>?
        get() = digitalInput.lastTransitionFrequencyMeasurement

    public val avgPeriod: UpdatableQuantity<Time> get() = digitalInput.avgPeriod

    override val isTransceiving: Trackable<Boolean> get() = digitalInput.isTransceivingBinaryState
    override val isFinalized: Boolean get() = digitalInput.isFinalized

    override fun startSampling() {
        digitalInput.startSamplingTransitionFrequency()
    }

    override fun stopTransceiving() {
        digitalInput.stopTransceiving()
    }

    override fun openSubscription(): ReceiveChannel<QuantityMeasurement<Frequency>> =
        digitalInput.openTransitionFrequencySubscription()

    override fun openProcessFailureSubscription(): ReceiveChannel<FailedMeasurement>? = null

    override fun finalize() {
        digitalInput.finalize()
    }

}