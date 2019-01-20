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

package org.tenkiv.kuantify.hardware.definitions.channel

import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.hardware.outputs.*
import javax.measure.quantity.*

/**
 * Class defining the basic features of an output which sends binary signals.
 */
@Suppress("LeakingThis")
abstract class DigitalOutput : DigitalChannel<DigitalDaqDevice>() {

    private val thisAsBinaryStateController = SimpleBinaryStateController(this)

    private val thisAsPwmController = SimplePwmController(this)

    private val thisAsFrequencyController = SimpleFrequencyController(this)

    abstract fun setOutput(state: BinaryState)

    /**
     * Activates this [DigitalOutput] for pulse width modulation.
     *
     * @param percent The percentage of the time the output is supposed to be active.
     */
    abstract fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>, panicOnFailure: Boolean): SettingResult

    /**
     * Activates this [DigitalInput] for transitioning states at [freq]
     *
     * @param freq The frequency of state change.
     */
    abstract fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>, panicOnFailure: Boolean): SettingResult

    override fun stopTransceiving() {
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