import io.kotlintest.specs.StringSpec
import org.tenkiv.nexus.data.*
import tec.uom.se.unit.Units.VOLT
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by tenkiv on 4/13/17.
 */


class TriggerTest: StringSpec() {
    init {

        "Trigger Test"{

            val gibberingSensor = GibberingSensor()

            var completed = false

            gibberingSensor.addTrigger({(it >= 3750.MILLIVOLT)}, {completed = true})

            Thread.sleep(10000)

            assert(completed)

        }
    }
}