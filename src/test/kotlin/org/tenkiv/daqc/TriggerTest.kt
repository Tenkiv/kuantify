package org.tenkiv.daqc

import io.kotlintest.specs.StringSpec
import org.tenkiv.physikal.core.milli
import org.tenkiv.physikal.core.volt

class TriggerTest : StringSpec() {

    init {

        "Trigger Test"{

            val gibberingSensor = AnalogGibberingSensor()

            var completed = false

            gibberingSensor.addTrigger(
                    {
                        (it.value >= 3750.milli.volt)
                    },
                    {
                        completed = true
                    })

            Thread.sleep(3000)

            assert(completed)
        }
    }
}