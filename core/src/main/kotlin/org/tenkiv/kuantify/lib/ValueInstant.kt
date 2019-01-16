package org.tenkiv.kuantify.lib

import kotlinx.serialization.*
import org.tenkiv.coral.*
import java.time.*

@Serializer(forClass = ValueInstant::class)
class ValueInstantSerializer<T : Any>(val valueSerializer: KSerializer<T>) : KSerializer<ValueInstantSerializer<T>>

data class PrimitiveValueInstant(val epochMilli: Long, val value: String) {

    inline fun <T> toValueInstant(deserializeValue: (String) -> T): ValueInstant<T> =
        deserializeValue(value) at Instant.ofEpochMilli(epochMilli)

}
