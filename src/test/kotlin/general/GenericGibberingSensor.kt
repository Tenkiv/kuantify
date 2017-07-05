package general

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.ScAnalogSensor
import kotlinx.coroutines.experimental.CommonPool
import org.tenkiv.coral.at
import org.tenkiv.physikal.core.milli
import org.tenkiv.physikal.core.volt
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.time.Instant
import java.util.*
import javax.measure.quantity.ElectricPotential


class GenericGibberingSensor : ScAnalogSensor<ElectricPotential>(EmptyAnalogInput(), 3.volt, 3.milli.volt) {

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                 broadcastChannel.offer(DaqcQuantity.of(random.nextInt(5000).milli.volt).at(Instant.now()))
            }
        },100,100)
    }

    override fun activate() {}

    override fun deactivate() {}

    override fun convertInput(ep: ComparableQuantity<ElectricPotential>): DaqcQuantity<ElectricPotential> {
        return DaqcQuantity.Companion.of(random.nextInt(5000).milli.volt)
    }

    fun cancel(){
        timer.cancel()
        broadcastChannel.close()
    }
}

class AnalogGibberingSensor:
        ScAnalogSensor<ElectricPotential>(EmptyAnalogInput(), 3.volt, 3.milli.volt) {

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                broadcastChannel.offer(
                            DaqcQuantity.of(random.nextInt(5000), MetricPrefix.MILLI(Units.VOLT)).at(Instant.now()))

            }
        },100,100)
    }

    override fun activate() {}

    override fun deactivate() {}

    override fun convertInput(ep: ComparableQuantity<ElectricPotential>): DaqcQuantity<ElectricPotential> {
        return DaqcQuantity.Companion.of(random.nextInt(5000).milli.volt)
    }

    fun cancel(){
        timer.cancel()
        broadcastChannel.close()
    }
}