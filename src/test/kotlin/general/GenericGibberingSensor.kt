package general

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.Sensor
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.launch
import org.tenkiv.physikal.core.milli
import org.tenkiv.physikal.core.volt
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.util.*
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 4/13/17.
 */
class GenericGibberingSensor : Sensor<DaqcValue>(emptyList<Input<DaqcValue>>()) {

    suspend override fun onUpdate(updatable: Updatable<DaqcValue>, value: DaqcValue) {}

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                launch(DAQC_CONTEXT) { broadcastChannel.send(DaqcQuantity.of(random.nextInt(5000).milli.volt)) }
            }
        },100,100)
    }

    fun cancel(){
        timer.cancel()
        broadcastChannel.close()
    }
}

class AnalogGibberingSensor:
        Sensor<DaqcQuantity<ElectricPotential>>(emptyList<Input<DaqcQuantity<ElectricPotential>>>()) {

    suspend override fun onUpdate(updatable: Updatable<DaqcQuantity<ElectricPotential>>,
                                  value: DaqcQuantity<ElectricPotential>) {
    }

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                launch(DAQC_CONTEXT) {
                    broadcastChannel.send(DaqcQuantity.of(random.nextInt(5000), MetricPrefix.MILLI(Units.VOLT)))
                }
            }
        },100,100)
    }

    fun cancel(){
        timer.cancel()
        broadcastChannel.close()
    }
}