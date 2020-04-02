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
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.trackable.*

/**
 * A simple implementation of a binary state controller.
 *
 * @param digitalOutput The [DigitalOutput] that is being controlled.
 */
public class SimpleBinaryStateController internal constructor(val digitalOutput: DigitalOutput<*>) :
    BinaryStateOutput, CoroutineScope by digitalOutput {

    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = digitalOutput.isTransceiving

    private val _broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    public override val updateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _broadcastChannel

    public override fun stopTransceiving() {
        digitalOutput.stopTransceiving()
    }

    public override fun setOutput(setting: BinaryState): SettingViability =
        digitalOutput.setOutputState(setting)

}