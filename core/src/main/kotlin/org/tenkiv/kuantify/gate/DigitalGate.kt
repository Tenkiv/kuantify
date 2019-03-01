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

package org.tenkiv.kuantify.gate

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import javax.measure.quantity.*

interface DigitalGate : DaqcGate<DigitalValue> {
    /**
     * The frequency with which pwm and transition frequency input and output will be averaged.
     * e.g. you want an output to be [BinaryState.High] 60% of the time. This will set if it's high 60% of the time
     * after 2 seconds, 1 second, 0.5 seconds, etc.
     */
    val avgFrequency: UpdatableQuantity<Frequency>

    /**
     * Gets if the channel is active for binary state.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    val isTransceivingBinaryState: InitializedTrackable<Boolean>

    /**
     * Gets if the channel is active for pulse width modulation.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    val isTransceivingPwm: InitializedTrackable<Boolean>

    /**
     * Gets if the channel is active for state transitions.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    val isTransceivingFrequency: InitializedTrackable<Boolean>

    val binaryStateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>

    /**
     * [ConflatedBroadcastChannel] for receiving pulse width modulation data.
     */
    val pwmBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>

    /**
     * [ConflatedBroadcastChannel] for receiving transition frequency data.
     */
    val transitionFrequencyBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
}

inline fun DigitalGate.onAnyTransceivingChange(
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
sealed class DigitalValue : DaqcData {

    override val size: Int
        get() = 1

    data class BinaryState(val state: org.tenkiv.kuantify.data.BinaryState) : DigitalValue() {

        override fun toDaqcValues(): List<DaqcValue> = listOf(state)

        companion object {
            internal const val TYPE_BYTE: Byte = 0
        }
    }

    data class Frequency(val frequency: DaqcQuantity<javax.measure.quantity.Frequency>) : DigitalValue() {

        override fun toDaqcValues(): List<DaqcValue> = listOf(frequency)

        companion object {
            internal const val TYPE_BYTE: Byte = 1
        }
    }

    data class Percentage(val percent: DaqcQuantity<Dimensionless>) : DigitalValue() {

        override fun toDaqcValues(): List<DaqcValue> = listOf(percent)

        companion object {
            internal const val TYPE_BYTE: Byte = 2
        }
    }

    //TODO: Redo this horrific abomination of a serialization hack
    @Serializer(forClass = DigitalValue::class)
    companion object {
        override val descriptor: SerialDescriptor = object : SerialClassDescImpl("DigitalGateValue") {
            init {
                addElement("type")
                addElement("value")
            }
        }

        override fun deserialize(decoder: Decoder): DigitalValue {
            val inp: CompositeDecoder = decoder.beginStructure(descriptor)
            var type: Byte = -1
            lateinit var value: String
            loop@ while (true) {
                when (val i = inp.decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_DONE -> break@loop
                    0 -> type = inp.decodeByteElement(descriptor, i)
                    1 -> value = inp.decodeStringElement(descriptor, i)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            inp.endStructure(descriptor)
            return when (type) {
                BinaryState.TYPE_BYTE -> BinaryState(org.tenkiv.kuantify.data.BinaryState.fromString(value))
                Frequency.TYPE_BYTE -> Frequency(DaqcQuantity.fromString(value))
                Percentage.TYPE_BYTE -> Percentage(DaqcQuantity.fromString(value))
                else -> throw SerializationException("Invalid type representation")
            }
        }

        override fun serialize(encoder: Encoder, obj: DigitalValue) {
            val compositeOutput: CompositeEncoder = encoder.beginStructure(descriptor)

            val type = when (obj) {
                is BinaryState -> BinaryState.TYPE_BYTE
                is Frequency -> Frequency.TYPE_BYTE
                is Percentage -> Percentage.TYPE_BYTE
            }

            val value = when (obj) {
                is BinaryState -> obj.state.toString()
                is Frequency -> obj.frequency.toString()
                is Percentage -> obj.percent.toString()
            }

            compositeOutput.encodeByteElement(descriptor, 0, type)
            compositeOutput.encodeStringElement(descriptor, 1, value)
            compositeOutput.endStructure(descriptor)
        }
    }
}