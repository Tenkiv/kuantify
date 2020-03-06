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

package org.tenkiv.kuantify.lib

import kotlinx.serialization.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import physikal.*
import java.time.*

public typealias Measurement = ValueInstant<DaqcValue>
public typealias BinaryStateMeasurement = ValueInstant<BinaryState>
public typealias QuantityMeasurement<QT> = ValueInstant<DaqcQuantity<QT>>

@Serializer(forClass = ValueInstant::class)
public class ValueInstantSerializer<T : Any>(val valueSerializer: KSerializer<T>) : KSerializer<ValueInstant<T>> {

    public override val descriptor: SerialDescriptor = SerialDescriptor("ValueInstantSerializer") {
            element("value", valueSerializer.descriptor)
            element("instant", InstantSerializer.descriptor)
    }

    public override fun serialize(encoder: Encoder, value: ValueInstant<T>) {
        val out = encoder.beginStructure(descriptor)
        out.encodeSerializableElement(descriptor, 0, valueSerializer, value.value)
        out.encodeSerializableElement(descriptor, 1, InstantSerializer, value.instant)
        out.endStructure(descriptor)
    }

    public override fun deserialize(decoder: Decoder): ValueInstant<T> {
        val inp = decoder.beginStructure(descriptor)
        lateinit var value: T
        lateinit var instant: Instant
        loop@ while (true) {
            when (val i = inp.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> value = inp.decodeSerializableElement(descriptor, i, valueSerializer)
                1 -> instant = inp.decodeSerializableElement(descriptor, i, InstantSerializer)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        inp.endStructure(descriptor)
        return value at instant
    }

}

private val binaryStateMeasurementSerializer: KSerializer<BinaryStateMeasurement> =
    ValueInstantSerializer(BinaryState.serializer())

public fun <T : Any> ValueInstant.Companion.serializer(valueSerializer: KSerializer<T>) =
    ValueInstantSerializer(valueSerializer)

public fun ValueInstant.Companion.binaryStateSerializer() = binaryStateMeasurementSerializer

public fun <QT : Quantity<QT>> ValueInstant.Companion.quantitySerializer() = serializer(DaqcQuantity.serializer<QT>())

@Serializer(forClass = Instant::class)
public object InstantSerializer : KSerializer<Instant> {

    public override val descriptor: SerialDescriptor = PrimitiveDescriptor("Instant", PrimitiveKind.LONG)

    public override fun deserialize(decoder: Decoder): Instant {
        return Instant.ofEpochMilli(decoder.decodeLong())
    }

    public override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilli())
    }

}

public data class PrimitiveValueInstant(public val epochMilli: Long, public val value: String) {

    public inline fun <T> toValueInstant(deserializeValue: (String) -> T): ValueInstant<T> =
        deserializeValue(value) at Instant.ofEpochMilli(epochMilli)

}