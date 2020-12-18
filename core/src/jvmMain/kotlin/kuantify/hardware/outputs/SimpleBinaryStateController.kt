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

package kuantify.hardware.outputs

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kuantify.data.*
import kuantify.gate.control.*
import kuantify.gate.control.output.*
import kuantify.hardware.channel.*
import kuantify.lib.*
import kuantify.trackable.*
import physikal.types.*

//TODO: inline
/**
 * A simple implementation of a binary state controller.
 *
 * @param digitalOutput The [DigitalOutput] that is being controlled.
 */
internal class SimpleBinaryStateController(val digitalOutput: DigitalOutput) :
    BinaryStateOutput, CoroutineScope by digitalOutput {

    override val valueFlow: SharedFlow<ValueInstant<BinaryState>>
        get() = digitalOutput.binaryStateFlow

    val avgPeriod: UpdatableQuantity<Time>
        get() = digitalOutput.avgPeriod

    override val isTransceiving: Trackable<Boolean>
        get() = digitalOutput.isTransceivingBinaryState

    override val isFinalized: Boolean
        get() = digitalOutput.isFinalized

    override fun stopTransceiving() {
        digitalOutput.stopTransceiving()
    }

    override suspend fun setOutputIfViable(setting: BinaryState): SettingViability =
        digitalOutput.setOutputStateIV(setting)


    override fun finalize() {
        digitalOutput.finalize()
    }
}