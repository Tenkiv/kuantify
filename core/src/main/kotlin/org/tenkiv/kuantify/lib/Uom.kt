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

package org.tenkiv.kuantify.lib

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import javax.measure.*
import javax.measure.quantity.*

@Serializer(forClass = ComparableQuantity::class)
object ComparableQuantitySerializer : KSerializer<ComparableQuantity<*>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("ComparableQuantity") {
        init {
            addElement("value")
            addElement("unit")
        }
    }

    override fun deserialize(decoder: Decoder): ComparableQuantity<*> {
        val inp: CompositeDecoder = decoder.beginStructure(descriptor)
        var value = 0.0
        lateinit var unit: String
        loop@ while (true) {
            when (val i = inp.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> value = inp.decodeDoubleElement(descriptor, i)
                1 -> unit = inp.decodeStringElement(descriptor, i)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        inp.endStructure(descriptor)
        return value withSymbol unit
    }

    override fun serialize(encoder: Encoder, obj: ComparableQuantity<*>) {
        val compositeOutput: CompositeEncoder = encoder.beginStructure(descriptor)
        //TODO: Probably change this to encodeSerializableElement so we can accommodate for numbers other than Double
        compositeOutput.encodeDoubleElement(descriptor, 0, obj.getValue().toDouble())
        compositeOutput.encodeStringElement(descriptor, 1, obj.getUnit().toString())
        compositeOutput.endStructure(descriptor)
    }
}

/**
 * Converts a [Quantity] of a [Frequency] to a [Time].
 */
fun Quantity<Frequency>.toPeriod(): Quantity<Time> = this.inverse().asType()

/**
 * Converts a [ComparableQuantity] of a [Frequency] to a [Time].
 */
fun ComparableQuantity<Frequency>.toPeriod(): ComparableQuantity<Time> = this.inverse(Time::class.java)