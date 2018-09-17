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

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.now
import org.tenkiv.kuantify.BinaryStateMeasurement
import org.tenkiv.kuantify.data.BinaryState
import org.tenkiv.kuantify.gate.control.output.BinaryStateOutput
import org.tenkiv.kuantify.gate.control.output.SettingResult
import org.tenkiv.kuantify.hardware.definitions.channel.DigitalOutput

/**
 * A simple implementation of a binary state controller.
 *
 * @param digitalOutput The [DigitalOutput] that is being controlled.
 */
class SimpleBinaryStateController internal constructor(val digitalOutput: DigitalOutput) :
    BinaryStateOutput, CoroutineScope by digitalOutput {

    /**
     * Denotes if the [DigitalOutput] is inverted; ie Off = [BinaryState.On]
     */
    @Volatile
    var inverted: Boolean = false

    override val isTransceiving: Boolean = digitalOutput.isTransceiving

    private val _broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    override val broadcastChannel: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _broadcastChannel

    override fun stopTransceiving() = digitalOutput.stopTransceiving()

    override fun setOutput(setting: BinaryState, panicOnFailure: Boolean): SettingResult {
        val result = if (!inverted) digitalOutput.setOutput(setting) else when (setting) {
            BinaryState.On -> digitalOutput.setOutput(BinaryState.Off, panicOnFailure)
            BinaryState.Off -> digitalOutput.setOutput(BinaryState.On, panicOnFailure)
        }

        if (result is SettingResult.Success) _broadcastChannel.offer(setting.now())

        return result
    }

}