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
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.acquire.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.trackable.*

//TODO: inline
/**
 * A simple simple implementation of a binary sensor
 *
 * @param digitalInput The [DigitalInput] that is being read from.
 */
internal class SimpleBinaryStateSensor(val digitalInput: DigitalInput) :
    BinaryStateInput, CoroutineScope by digitalInput {
    override val valueOrNull: BinaryStateMeasurement?
        get() = digitalInput.lastStateMeasurement

    override val isTransceiving: Trackable<Boolean> get() = digitalInput.isTransceivingBinaryState
    override val updateRate: UpdateRate get() = digitalInput.updateRate
    override val isFinalized: Boolean get() = digitalInput.isFinalized

    override fun startSampling() {
        digitalInput.startSamplingBinaryState()
    }

    override fun stopTransceiving() {
        digitalInput.stopTransceiving()
    }

    override fun openSubscription(): ReceiveChannel<ValueInstant<BinaryState>> =
        digitalInput.openBinaryStateSubscription()

    override fun openProcessFailureSubscription(): ReceiveChannel<FailedMeasurement>? = null

    override fun finalize() {
        digitalInput.finalize()
    }
}