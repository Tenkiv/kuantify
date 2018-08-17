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

import org.tenkiv.daqc.data.DaqcQuantity
import org.tenkiv.daqc.gate.receive.input.BinaryStateInput
import org.tenkiv.daqc.hardware.inputs.SimpleBinaryStateSensor
import org.tenkiv.daqc.hardware.inputs.SimpleDigitalFrequencySensor
import org.tenkiv.daqc.hardware.inputs.SimplePwmSensor
import javax.measure.quantity.Frequency

abstract class DigitalInput : DigitalChannel(), BinaryStateInput {

    override val isActive get() = super.isActive

    private val thisAsBinaryStateSensor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleBinaryStateSensor(this)
    }

    private val thisAsTransitionFrequencyInput by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleDigitalFrequencySensor(this)
    }

    private val thisAsPwmInput by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimplePwmSensor(this)
    }

    abstract fun activateForTransitionFrequency(avgFrequency: DaqcQuantity<Frequency>)

    abstract fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>)

    open fun activateForCurrentState() = activate()

    fun asBinaryStateSensor(inverted: Boolean = false) = thisAsBinaryStateSensor.apply {
        this.inverted = inverted
    }

    fun asTransitionFrequencySensor(avgFrequency: DaqcQuantity<Frequency>) = thisAsTransitionFrequencyInput.apply {
        this.avgFrequency = avgFrequency
    }

    fun asPwmSensor(avgFrequency: DaqcQuantity<Frequency>) = thisAsPwmInput.apply {
        this.avgFrequency = avgFrequency
    }
}