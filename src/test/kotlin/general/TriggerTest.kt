package general

import org.tenkiv.nexus.data.MILLIVOLT
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 4/13/17.
 */
class TriggerTest: io.kotlintest.specs.StringSpec() {

    init {

        "Trigger Test"{

            val gibberingSensor = GenericGibberingSensor()

            var completed = false

            gibberingSensor.addTrigger(
                    {println("Trying Trigger with latestValue $it");
                        (it as com.tenkiv.daqc.DaqcValue.DaqcQuantity<ElectricPotential> >= 3750.MILLIVOLT)
                    },
                    {println("Trigger Fired");completed = true})

            Thread.sleep(10000)

            assert(completed)

        }
    }
}