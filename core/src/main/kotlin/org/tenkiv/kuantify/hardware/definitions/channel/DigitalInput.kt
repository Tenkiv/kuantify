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

import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.hardware.inputs.*
import tec.units.indriya.*
import javax.measure.quantity.*

/**
 * Class defining the basic features of an input which reads binary signals.
 */
@Suppress("LeakingThis")
abstract class DigitalInput : DigitalChannel<DigitalDaqDevice>(), RatedTrackable<DigitalChannelValue> {

    private val thisAsBinaryStateSensor = SimpleBinaryStateSensor(this)

    private val thisAsTransitionFrequencyInput = SimpleDigitalFrequencySensor(this)

    private val thisAsPwmSensor = SimplePwmSensor(this)

    /**
     * Activates this channel to gather data for transition frequency averaged over a certain period of time.
     *
     * @param avgFrequency The period of time to average the transition frequency.
     */
    abstract fun startSamplingTransitionFrequency()

    /**
     * Activates this channel to gather data for PWM averaged over a certain period of time.
     *
     * @param avgFrequency The period of time to average the PWM frequency.
     */
    abstract fun startSamplingPwm()

    /**
     * Activates the channel to receive data about the current state of the [DigitalInput]
     */
    abstract fun startSamplingBinaryState()

    /**
     * Creates a [SimpleBinaryStateSensor] with the input being this channel.
     *
     * @param inverted If the channel has inverted values, ie Low == [BinaryState.High]. Default is false.
     * @return A [SimpleBinaryStateSensor] with the input as this channel.
     */
    fun asBinaryStateSensor() = thisAsBinaryStateSensor

    /**
     * Creates a [SimpleDigitalFrequencySensor] with the input being this channel.
     *
     * @param avgFrequency The average period of time over which to average.
     * @return A [SimpleDigitalFrequencySensor] with the input as this channel.
     */
    fun asTransitionFrequencySensor(avgFrequency: ComparableQuantity<Frequency>): SimpleDigitalFrequencySensor {
        this.avgFrequency.set(avgFrequency)
        return thisAsTransitionFrequencyInput
    }

    /**
     * Creates a [SimplePwmSensor] with the input being this channel.
     *
     * @param avgFrequency The average period of time over which to average.
     * @return A [SimplePwmSensor] with the input as this channel.
     */
    fun asPwmSensor(avgFrequency: ComparableQuantity<Frequency>): SimplePwmSensor {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmSensor
    }
}