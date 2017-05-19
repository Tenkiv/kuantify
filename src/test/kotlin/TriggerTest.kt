import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import com.tenkiv.daqc.hardware.definitions.device.Device
import io.kotlintest.specs.StringSpec
import org.tenkiv.nexus.data.*
import tec.uom.se.unit.Units.VOLT
import java.util.concurrent.locks.ReentrantLock
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 4/13/17.
 */
class TriggerTest: StringSpec() {
    init {
        "Trigger Test"{

            val gibberingSensor = GenericGibberingSensor()

            var completed = false

            gibberingSensor.addTrigger({println(it);(it as DaqcValue.Quantity<ElectricPotential> >= 3750.MILLIVOLT)}, {completed = true})

            Thread.sleep(10000)

            assert(completed)

        }
    }
}