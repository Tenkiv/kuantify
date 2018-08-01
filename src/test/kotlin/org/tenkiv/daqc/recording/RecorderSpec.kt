package org.tenkiv.daqc.recording

import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.now
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.*
import org.tenkiv.daqc.recording.quantity.pairWithNewRecorder
import org.tenkiv.daqc.recording.binary.pairWithNewRecorder
import java.lang.Thread.sleep

class RecorderSpec : StringSpec({

    val analogGibberingSensor = AnalogGibberingSensor().apply {
        activate()
    }
    val digitalGibberingSensor = DigitalGibberingSensor().apply {
        activate()
    }

    "Recording daqc values in memory" {
        val analogRecorder = analogGibberingSensor.pairWithNewRecorder(memoryDuration = StorageDuration.For(10L.secondsSpan),
            diskDuration = StorageDuration.None).recorder

        val digitalRecorder = digitalGibberingSensor.pairWithNewRecorder(memoryDuration = StorageDuration.For(10L.secondsSpan),
        diskDuration = StorageDuration.None).recorder

        sleep(12_000)

        val analogData = analogRecorder.getDataInMemory()
        analogData.last().instant.epochSecond -
                analogData.first().instant.epochSecond shouldBe (10.0 plusOrMinus 1.0)

        val digitalData = digitalRecorder.getDataInMemory()
        digitalData.last().instant.epochSecond -
                digitalData.first().instant.epochSecond shouldBe (10.0 plusOrMinus 1.0)

    }

    "Recording daqc values in disk" {

        val analogRecorder = analogGibberingSensor.pairWithNewRecorder(memoryDuration = StorageDuration.None,
                diskDuration = StorageDuration.For(10L.secondsSpan)).recorder

        val digitalRecorder = digitalGibberingSensor.pairWithNewRecorder(memoryDuration = StorageDuration.None,
                diskDuration = StorageDuration.For(10L.secondsSpan)).recorder

        sleep(13_000)

        runBlocking {

            val analogData = analogRecorder.getAllData()
            analogData.last().instant.epochSecond -
                    analogData.first().instant.epochSecond shouldBe (10.0 plusOrMinus 2.0)

            val digitalData = digitalRecorder.getAllData()
            digitalData.last().instant.epochSecond -
                    digitalData.first().instant.epochSecond shouldBe (10.0 plusOrMinus 2.0)

        }

    }

    "Recording random JSON object in disk" {

        val subthing = SubThing("some info", 42, listOf("1", "2", "3"))

        val recorder = object : Updatable<ValueInstant<Thing>> {

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

            data.last().instant.epochSecond -
                    data.first().instant.epochSecond shouldBe (10.0 plusOrMinus 1.5)
    }
    }
})

@Serializable
private data class Thing(val name: String, val subStuff: SubThing, val age: Int, val otherAttrs: Array<SubThing>)

@Serializable
private data class SubThing(val info: String, val bar: Int, val moreInfo: List<String>)