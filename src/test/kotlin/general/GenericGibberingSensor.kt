package general

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.Sensor
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.util.*
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 4/13/17.
 */
class GenericGibberingSensor : Sensor<DaqcValue>(emptyList<Input<DaqcValue>>()) {

    override val onDataReceived: suspend (Updatable<DaqcValue>) -> Unit
        get() = {}


    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                latestValue = DaqcValue.Quantity.of(random.nextInt(5), Units.VOLT)
            }
        },100,100)
    }

    fun cancel(){
        timer.cancel()
    }
}

class AnalogGibberingSensor: Sensor<DaqcValue.Quantity<ElectricPotential>>(emptyList<Input<DaqcValue.Quantity<ElectricPotential>>>()) {
    override val onDataReceived: suspend (Updatable<DaqcValue.Quantity<ElectricPotential>>) -> Unit
        get() = {}


    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                latestValue = DaqcValue.Quantity.of(random.nextInt(5000), MetricPrefix.MILLI(Units.VOLT))
            }
        },100,100)
    }

    fun cancel(){
        timer.cancel()
    }
}