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

package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.BinaryStateOutput
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.outputs.SimpleBinaryStateController
import org.tenkiv.daqc.hardware.outputs.SimpleFrequencyController
import org.tenkiv.daqc.hardware.outputs.SimplePwmController
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


abstract class DigitalOutput : DigitalChannel(), BinaryStateOutput {

    override val isActive get() = super.isActive

    private val thisAsBinaryStateController by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleBinaryStateController(this)
    }

    private val thisAsPwmController by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimplePwmController(this)
    }

    private val thisAsFrequencyController by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleFrequencyController(this)
    }

    abstract fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>)

    abstract fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>)

    override fun deactivate() = setOutput(BinaryState.Off)

    fun asBinaryStateController(inverted: Boolean = false) = thisAsBinaryStateController.apply {
        this.inverted = inverted
    }

    fun asPwmController() = thisAsPwmController

    fun asFrequencyController() = thisAsFrequencyController


}