package general

import com.tenkiv.daqc.DaqcQuantity
import io.kotlintest.specs.StringSpec
import org.tenkiv.coral.ValueInstant
import org.tenkiv.physikal.core.milli
import org.tenkiv.physikal.core.volt

import javax.measure.quantity.ElectricPotential

class TriggerTest: StringSpec() {

    init {

        "Trigger Test"{

            val gibberingSensor = AnalogGibberingSensor()

            var completed = false

            gibberingSensor.addTrigger(
                    {println("Trying Trigger with latestValue ${it.value}")
                        (it.value >= 3750.milli.volt)
                    },
                    {println("Trigger Fired");completed = true})

            Thread.sleep(3000)

            assert(completed)
        }
    }
}