package org.tenkiv.daqc.recording

import io.kotlintest.matchers.plusOrMinus
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.AnalogGibberingSensor
import org.tenkiv.daqc.DigitalGibberingSensor
import org.tenkiv.daqc.recording.binary.newRecorder
import org.tenkiv.daqc.recording.quantity.newRecorder
import java.lang.Thread.sleep

class RecorderSpec : StringSpec({

    val analogGibberingSensor = AnalogGibberingSensor().apply {
        activate()
    }
    val digitalGibberingSensor = DigitalGibberingSensor().apply {
        activate()
    }

    "Recorder write daqc value to memory" {

        val analogRecorder = analogGibberingSensor.newRecorder(memoryDuration = StorageDuration.For(10L.secondsSpan),
                diskDuration = StorageDuration.None)

        val digitalRecorder = digitalGibberingSensor.newRecorder(memoryDuration = StorageDuration.For(10L.secondsSpan),
                diskDuration = StorageDuration.None)

        sleep(12_000)

        val analogDataInMemory = analogRecorder.getDataInMemory()
        analogDataInMemory.last().instant.epochSecond -
                analogDataInMemory.first().instant.epochSecond shouldBe (10.0 plusOrMinus 1.0)

    }

    "Recorder read daqc value from memory" {

    }

    "Recorder write daqc value to disk" {

    }

    "Recorder read daqc value from disk" {

    }

    "Recorder write random JSON object to disk" {

    }

    "Recorder read random JSON object from disk" {

    }

})