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

package kuantify.lib

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kuantify.data.*
import physikal.*

public typealias Measurement = ValueInstant<DaqcValue>
public typealias BinaryStateMeasurement = ValueInstant<BinaryState>
public typealias QuantityMeasurement<QT> = ValueInstant<DaqcQuantity<QT>>

public infix fun <T> T.at(instant: Instant): ValueInstant<T> = ValueInstant(this, instant)

public fun <T> T.now(): ValueInstant<T> = ValueInstant(this, Clock.System.now())

public fun <T> Iterable<ValueInstant<T>>.getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<T>> =
    this.filter { it.instant in instantRange }

@Serializable(with = ValueInstantSerializer::class)
public data class ValueInstant<out T>(val value: T, val instant: Instant)

//TODO: Need to look into instant more. Serializing only as milliseconds may actually be losing resolution and we
// may need that resolution.
public class ValueInstantSerializer<T : Any>(public val valueSerializer: KSerializer<T>) :
    KSerializer<ValueInstant<T>> {

    public override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ValueInstantSerializer") {
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
                CompositeDecoder.DECODE_DONE -> break@loop
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

public fun ValueInstant.Companion.binaryStateSerializer(): KSerializer<ValueInstant<BinaryState>> =
    binaryStateMeasurementSerializer

public fun <QT : Quantity<QT>> ValueInstant.Companion.quantitySerializer(): KSerializer<ValueInstant<DaqcQuantity<QT>>> =
    serializer(DaqcQuantity.serializer())

public object InstantSerializer : KSerializer<Instant> {

    public override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    public override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochMilliseconds(decoder.decodeLong())
    }

    public override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }

}