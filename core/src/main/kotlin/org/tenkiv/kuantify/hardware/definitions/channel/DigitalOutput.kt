/*
 * Copyright 2019 Tenkiv, Inc.
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
 *
 */

package org.tenkiv.kuantify.hardware.definitions.channel

import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.hardware.outputs.*
import tec.units.indriya.*
import javax.measure.quantity.*

/**
 * Class defining the basic features of an output which sends binary signals.
 */
interface DigitalOutput : DigitalChannel<DigitalDaqDevice> {

    fun setOutputState(
        state: BinaryState,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult

    /**
     * Activates this [DigitalOutput] for pulse width modulation.
     *
     * @param percent The percentage of the time the output is supposed to be active.
     */
    fun pulseWidthModulate(
        percent: ComparableQuantity<Dimensionless>,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult

    /**
     * Activates this [DigitalInput] for transitioning states at [freq]
     *
     * @param freq The frequency of state change.
     */
    fun sustainTransitionFrequency(
        freq: ComparableQuantity<Frequency>,
        panicOnFailure: Boolean = ControlGate.DEFAULT_PANIC_ON_FAILURE
    ): SettingResult

    override fun stopTransceiving() {
        setOutputState(BinaryState.Low)
    }

    /**
     * Ease of use function to create a [SimpleBinaryStateController] from this [DigitalOutput].
     *
     * @param inverted Denotes if [BinaryState.ON] is Low as well as the inverse. By default false.
     * @return A [SimpleBinaryStateController] with this [DigitalOutput] as the controlled output.
     */
    fun asBinaryStateController(): BinaryStateOutput

    /**
     * Ease of use function to create a [SimplePwmController] from this [DigitalOutput].
     *
     * @return A [SimplePwmController] with this [DigitalOutput] as the controlled output.
     */
    fun asPwmController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Dimensionless>

    /**
     * Ease of use function to create a [SimpleFrequencyController] from this [DigitalOutput].
     *
     * @return A [SimpleFrequencyController] with this [DigitalOutput] as the controlled output.
     */
    fun asFrequencyController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Frequency>
}