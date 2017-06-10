package general

import com.tenkiv.daqc.DaqcValue
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.nexus.data.*
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
                        (it as com.tenkiv.daqc.DaqcValue.Quantity<ElectricPotential> >= 3750.MILLIVOLT)},
                    {println("Trigger Fired");completed = true})

            Thread.sleep(10000)

            assert(completed)

        }
    }
}