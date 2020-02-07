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
 */

package org.tenkiv.kuantify.gate

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import physikal.types.*

public interface DigitalGate : DaqcGate<DigitalValue> {
    /**
     * The frequency with which pwm and transition frequency input and output will be averaged.
     * e.g. you want an output to be [BinaryState.High] 60% of the time. This will set if it's high 60% of the time
     * after 2 seconds, 1 second, 0.5 seconds, etc.
     */
    public val avgFrequency: UpdatableQuantity<Frequency>

    /**
     * Gets if the channel is active for binary state.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    public val isTransceivingBinaryState: InitializedTrackable<Boolean>

    /**
     * Gets if the channel is active for pulse width modulation.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    public val isTransceivingPwm: InitializedTrackable<Boolean>

    /**
     * Gets if the channel is active for state transitions.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    public val isTransceivingFrequency: InitializedTrackable<Boolean>

    public val binaryStateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>

    /**
     * [ConflatedBroadcastChannel] for receiving pulse width modulation data.
     */
    public val pwmBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>

    /**
     * [ConflatedBroadcastChannel] for receiving transition frequency data.
     */
    public val transitionFrequencyBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
}

public inline fun DigitalGate.onAnyTransceivingChange(
    crossinline block: (anyTransceiving: Boolean) -> Unit
) {
    launch {
        isTransceivingBinaryState.updateBroadcaster.consumeEach {
            block(
                isTransceivingBinaryState.value ||
                        isTransceivingFrequency.value ||
                        isTransceivingFrequency.value
            )
        }
    }
    launch {
        isTransceivingPwm.updateBroadcaster.consumeEach {
            block(
                isTransceivingBinaryState.value ||
                        isTransceivingFrequency.value ||
                        isTransceivingFrequency.value
            )
        }
    }
    launch {
        isTransceivingFrequency.updateBroadcaster.consumeEach {
            block(
                isTransceivingBinaryState.value ||
                        isTransceivingFrequency.value ||
                        isTransceivingFrequency.value
            )
        }
    }
}

@Serializable
public sealed class DigitalValue : DaqcData {

    override val size: Int
        get() = 1

    @Serializable
    public data class BinaryState(val state: org.tenkiv.kuantify.data.BinaryState) : DigitalValue() {

        override fun toDaqcValues(): List<DaqcValue> = listOf(state)

        companion object {
            internal const val TYPE_BYTE: Byte = 0
        }
    }

    @Serializable
    public data class Frequency(
        @Serializable(with = DaqcQuantitySerializer::class)
        val frequency: DaqcQuantity<org.tenkiv.kuantify.lib.physikal.Frequency>
    ) : DigitalValue() {

        override fun toDaqcValues(): List<DaqcValue> = listOf(frequency)

        companion object {
            internal const val TYPE_BYTE: Byte = 1
        }
    }

    @Serializable
    public data class Percentage(
        @Serializable(with = DaqcQuantitySerializer::class)
        val percent: DaqcQuantity<Dimensionless>
    ) : DigitalValue() {

        override fun toDaqcValues(): List<DaqcValue> = listOf(percent)

        companion object {
            internal const val TYPE_BYTE: Byte = 2
        }
    }
}