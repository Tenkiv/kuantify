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

package org.tenkiv.kuantify.gate

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.trackable.*
import physikal.types.*

public interface DigitalGate : DaqcGate {
    /**
     * The period with which pwm and transition frequency input and output will be averaged.
     * e.g. you want an output to be [BinaryState.High] 60% of the time. This will set if it's high 60% of the time
     * after 2 seconds, 1 second, 0.5 seconds, etc.
     */
    public val avgPeriod: UpdatableQuantity<Time>
    public val isTransceivingBinaryState: Trackable<Boolean>
    public val isTransceivingPwm: Trackable<Boolean>
    public val isTransceivingFrequency: Trackable<Boolean>

    public fun openBinaryStateSubscription(): ReceiveChannel<BinaryStateMeasurement>

    public fun openPwmSubscription(): ReceiveChannel<QuantityMeasurement<Dimensionless>>

    public fun openTransitionFrequencySubscription(): ReceiveChannel<QuantityMeasurement<Frequency>>
}

public inline fun DigitalGate.onAnyTransceivingChange(
    crossinline block: (anyTransceiving: Boolean) -> Unit
) {
    launch {
        isTransceivingBinaryState.onEachUpdate {
            block(
                isTransceivingBinaryState.get() ||
                        isTransceivingFrequency.get() ||
                        isTransceivingFrequency.get()
            )
        }
    }
    launch {
        isTransceivingPwm.onEachUpdate {
            block(
                isTransceivingBinaryState.get() ||
                        isTransceivingFrequency.get() ||
                        isTransceivingFrequency.get()
            )
        }
    }
    launch {
        isTransceivingFrequency.onEachUpdate {
            block(
                isTransceivingBinaryState.get() ||
                        isTransceivingFrequency.get() ||
                        isTransceivingFrequency.get()
            )
        }
    }
}
