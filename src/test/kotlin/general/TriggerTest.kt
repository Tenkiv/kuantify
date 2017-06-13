package general

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.milli
import com.tenkiv.daqc.volt
import io.kotlintest.specs.StringSpec

import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 4/13/17.
 */
class TriggerTest: StringSpec() {

    init {

        "Trigger Test"{

            val gibberingSensor = GenericGibberingSensor()

            var completed = false

            gibberingSensor.addTrigger(
                    {println("Trying Trigger with latestValue $it");
                        (it as DaqcQuantity<ElectricPotential> >= 3750.milli.volt)
                    },
                    {println("Trigger Fired");completed = true})

            Thread.sleep(10000)

            assert(completed)

        }
    }
}