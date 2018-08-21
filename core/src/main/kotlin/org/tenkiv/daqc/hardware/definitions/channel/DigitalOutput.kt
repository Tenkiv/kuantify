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

import org.tenkiv.daqc.data.BinaryState
import org.tenkiv.daqc.data.DaqcQuantity
import org.tenkiv.daqc.gate.control.output.BinaryStateOutput
import org.tenkiv.daqc.gate.control.output.SettingViability
import org.tenkiv.daqc.hardware.outputs.SimpleBinaryStateController
import org.tenkiv.daqc.hardware.outputs.SimpleFrequencyController
import org.tenkiv.daqc.hardware.outputs.SimplePwmController
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

/**
 * Class defining the basic features of an output which sends binary signals.
 */
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

    /**
     * Activates this [DigitalOutput] for pulse width modulation.
     *
     * @param percent The percentage of the time the output is supposed to be active.
     */
    abstract fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>): SettingViability

    /**
     * Activates this [DigitalInput] for transitioning states at [freq]
     *
     * @param freq The frequency of state change.
     */
    abstract fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>): SettingViability

    override fun deactivate() {
        setOutput(BinaryState.Off)
    }

    /**
     * Ease of use function to create a [SimpleBinaryStateController] from this [DigitalOutput].
     *
     * @param inverted Denotes if [BinaryState.ON] is Off as well as the inverse. By default false.
     * @return A [SimpleBinaryStateController] with this [DigitalOutput] as the controlled output.
     */
    fun asBinaryStateController(inverted: Boolean = false) = thisAsBinaryStateController.apply {
        this.inverted = inverted
    }

    /**
     * Ease of use function to create a [SimplePwmController] from this [DigitalOutput].
     *
     * @return A [SimplePwmController] with this [DigitalOutput] as the controlled output.
     */
    fun asPwmController() = thisAsPwmController

    /**
     * Ease of use function to create a [SimpleFrequencyController] from this [DigitalOutput].
     *
     * @return A [SimpleFrequencyController] with this [DigitalOutput] as the controlled output.
     */
    fun asFrequencyController() = thisAsFrequencyController
}