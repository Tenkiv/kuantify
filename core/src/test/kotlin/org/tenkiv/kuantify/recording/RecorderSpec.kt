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

package org.tenkiv.kuantify.recording

import io.kotlintest.specs.StringSpec
import kotlinx.serialization.Serializable
import org.tenkiv.kuantify.AnalogGibberingSensor
import org.tenkiv.kuantify.DigitalGibberingSensor

class RecorderSpec : StringSpec({

    val analogGibberingSensor = AnalogGibberingSensor().apply {
        startSampling()
    }
    val digitalGibberingSensor = DigitalGibberingSensor().apply {
        startSampling()
    }

    /*"Recording daqc values in memory" {
        val analogRecorder = analogGibberingSensor.pairWithNewRecorder(memoryDuration = StorageDuration.For(10L.secondsSpan),
            diskDuration = StorageDuration.None).recorder

        val digitalRecorder = digitalGibberingSensor.pairWithNewRecorder(memoryDuration = StorageDuration.For(10L.secondsSpan),
        diskDuration = StorageDuration.None).recorder

        sleep(12_000)

        val analogData = analogRecorder.getDataInMemory()
        analogData.last().instant.epochSecond -
                analogData.first().instant.epochSecond.toDouble() shouldBe (10.0 plusOrMinus 1.0)

        val digitalData = digitalRecorder.getDataInMemory()
        digitalData.last().instant.epochSecond -
                digitalData.first().instant.epochSecond.toDouble() shouldBe (10.0 plusOrMinus 1.0)

    }*/

    "Recording daqc values in disk" {

       /* val analogRecorder = analogGibberingSensor.pairWithNewRecorder(memoryDuration = StorageDuration.None,
                diskDuration = StorageDuration.For(10L.secondsSpan)).recorder

        val digitalRecorder = digitalGibberingSensor.pairWithNewRecorder(memoryDuration = StorageDuration.None,
                diskDuration = StorageDuration.For(10L.secondsSpan)).recorder

        sleep(13_000)

        runBlocking {

            val analogData = analogRecorder.getAllData()
            analogData.last().instant.epochSecond -
                    analogData.first().instant.epochSecond.toDouble() shouldBe (10.0 plusOrMinus 2.0)

            val digitalData = digitalRecorder.getAllData()
            digitalData.last().instant.epochSecond -
                    digitalData.first().instant.epochSecond.toDouble() shouldBe (10.0 plusOrMinus 2.0)

        }*/

    }

    /*"Recording random JSON object in disk" {

        val subthing = SubThing("some info", 42, listOf("1", "2", "3"))

        val recorder = object : Updatable<ValueInstant<Thing>> {
            override val coroutineContext: CoroutineContext = Job()

            override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<Thing>>()

            init {
                launch {
                    while (true) {
                        broadcastChannel.send(Thing(
                                "a name",
                                subthing,
                                4,
                                arrayOf(subthing, subthing, subthing)
                        ).now())
                        delay(150)
                    }
                }
            }
        }.pairWithNewRecorder(
                memoryDuration = StorageDuration.None,
                diskDuration = StorageDuration.For(10L.secondsSpan),
                valueSerializer = {
                    JSON.stringify(it)
                },
                valueDeserializer = {
                    JSON.parse(it)
                }
        ).recorder

        sleep(12_000)

        runBlocking {
            val data = recorder.getAllData()

            println(data)

            data.last().instant.epochSecond.toDouble() -
                    data.first().instant.epochSecond.toDouble() shouldBe (10.0 plusOrMinus 1.5)
    }
    }*/
})

@Serializable
private data class Thing(val name: String, val subStuff: SubThing, val age: Int, val otherAttrs: Array<SubThing>)

@Serializable
private data class SubThing(val info: String, val bar: Int, val moreInfo: List<String>)