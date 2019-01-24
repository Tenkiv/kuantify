package org.tenkiv.kuantify.lib

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import org.tenkiv.coral.*
import java.time.*

@Serializer(forClass = ValueInstant::class)
class ValueInstantSerializer<T : Any>(val valueSerializer: KSerializer<T>) : KSerializer<ValueInstant<T>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("BinaryPayload") {
        init {
            addElement("value")
            addElement("instant")
        }
    }

    override fun serialize(encoder: Encoder, obj: ValueInstant<T>) {
        val out = encoder.beginStructure(descriptor)
        out.encodeSerializableElement(descriptor, 0, valueSerializer, obj.value)
        out.encodeSerializableElement(descriptor, 1, InstantSerializer, obj.instant)
        out.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): ValueInstant<T> {
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

@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor = LongDescriptor.withName("Instant")

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.ofEpochMilli(decoder.decodeLong())
    }

    override fun serialize(encoder: Encoder, obj: Instant) {
        encoder.encodeLong(obj.toEpochMilli())
    }

}

data class PrimitiveValueInstant(val epochMilli: Long, val value: String) {

    inline fun <T> toValueInstant(deserializeValue: (String) -> T): ValueInstant<T> =
        deserializeValue(value) at Instant.ofEpochMilli(epochMilli)

}
