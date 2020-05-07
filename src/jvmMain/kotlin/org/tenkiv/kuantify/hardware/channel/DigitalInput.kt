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

package org.tenkiv.kuantify.hardware.channel

import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.hardware.inputs.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import physikal.*
import physikal.types.*

/**
 * Class defining the basic features of an input which reads binary signals.
 */
public interface DigitalInput : DeviceDigitalGate {
    public override val device: DigitalDaqDevice

    public val lastStateMeasurement: BinaryStateMeasurement?

    public val lastPwmMeasurement: QuantityMeasurement<Dimensionless>?

    public val lastTransitionFrequencyMeasurement: QuantityMeasurement<Frequency>?

    /**
     * Activates this channel to gather data for transition frequency averaged over a certain period of time.
     */
    public fun startSamplingTransitionFrequency()

    /**
     * Activates this channel to gather data for PWM averaged over a certain period of time.
     */
    public fun startSamplingPwm()

    /**
     * Activates the channel to receive data about the current state of the [DigitalInput]
     */
    public fun startSamplingBinaryState()

}

/**
 * Creates a [SimpleBinaryStateSensor] with the input being this channel.
 *
 * @return A [SimpleBinaryStateSensor] with the input as this channel.
 */
public fun DigitalInput.asBinaryStateSensor(): BinaryStateInput = SimpleBinaryStateSensor(this)

/**
 * Creates a [SimplePwmSensor] with the input being this channel.
 *
 * @param avgPeriod The average period of time over which to average.
 * @return A [SimplePwmSensor] with the input as this channel.
 */
public fun DigitalInput.asPwmSensor(avgPeriod: Quantity<Time>): QuantityInput<Dimensionless> {
    this.avgPeriod.set(avgPeriod)
    return SimplePwmSensor(this)
}

/**
 * Creates a [SimpleDigitalFrequencySensor] with the input being this channel.
 *
 * @param avgPeriod The average period of time over which to average.
 * @return A [SimpleDigitalFrequencySensor] with the input as this channel.
 */
public fun DigitalInput.asTransitionFrequencySensor(avgPeriod: Quantity<Time>): QuantityInput<Frequency> {
    this.avgPeriod.set(avgPeriod)
    return SimpleDigitalFrequencySensor(this)
}