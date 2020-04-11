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

package org.tenkiv.kuantify.hardware.outputs

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.trackable.*
import physikal.types.*

//TODO: inline
/**
 * A simple implementation of a binary transition frequency controller.
 *
 * @param digitalOutput The [DigitalOutput] that is being controlled.
 */
public class SimpleFrequencyController internal constructor(val digitalOutput: DigitalOutput) :
    QuantityOutput<Frequency>, CoroutineScope by digitalOutput {
    override val valueOrNull: ValueInstant<DaqcQuantity<Frequency>>?
        get() = digitalOutput.lastTransitionFrequencySetting

    val avgPeriod: UpdatableQuantity<Time>
        get() = digitalOutput.avgPeriod

    override val isTransceiving: InitializedTrackable<Boolean>
        get() = digitalOutput.isTransceivingBinaryState

    override val isFinalized: InitializedTrackable<Boolean>
        get() = digitalOutput.isFinalized

    override fun openSubscription(): ReceiveChannel<ValueInstant<DaqcQuantity<Frequency>>> =
        digitalOutput.openTransitionFrequencySubscription()

    override suspend fun stopTransceiving() {
        digitalOutput.stopTransceiving()
    }

    override suspend fun setOutputIfViable(setting: DaqcQuantity<Frequency>): SettingViability =
        digitalOutput.sustainTransitionFrequency(setting)


    override fun finalize() {
        digitalOutput.finalize()
    }

}