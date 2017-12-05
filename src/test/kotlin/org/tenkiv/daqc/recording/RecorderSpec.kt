package org.tenkiv.daqc.recording

import io.kotlintest.specs.StringSpec
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.AnalogGibberingSensor
import org.tenkiv.daqc.DigitalGibberingSensor
import java.lang.Thread.sleep

class RecorderSpec : StringSpec({

    val analogGibberingSensor = AnalogGibberingSensor().apply {
        activate()
    }
    val digitalGibberingSensor = DigitalGibberingSensor().apply {
        activate()
    }

    "Recorder write daqc value to memory" {

        analogGibberingSensor.pairWithNewQuantityRecorder(memoryDuration = StorageDuration.For(10L.secondsSpan),
                diskDuration = StorageDuration.None)

        sleep(11_000)


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