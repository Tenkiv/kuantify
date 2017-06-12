package general

import com.tenkiv.daqc.DaqcQuantity
import io.kotlintest.specs.StringSpec
import org.tenkiv.nexus.data.MILLIVOLT
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
                        (it as DaqcQuantity<ElectricPotential> >= 3750.MILLIVOLT)
                    },
                    {println("Trigger Fired");completed = true})

            Thread.sleep(10000)

            assert(completed)

        }
    }
}