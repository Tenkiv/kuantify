import com.tenkiv.daqc.DataQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.Sensor
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.hardware.definitions.device.Device
import org.tenkiv.nexus.data.MILLIVOLT
import org.tenkiv.nexus.data.VOLT
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 4/13/17.
 */
class GibberingSensor: Sensor<DataQuantity<ElectricPotential>>(emptyList<Input<DaqcValue>>()) {

    val random = Random()

    init {
        val timer = Timer(false)

        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                //println("Gibbering $gib")
                value = DataQuantity(random.nextInt(5).VOLT)
                onDataUpdate(this@GibberingSensor)
            }
        },100,100)
    }

    override val onDataReceived: UpdatableListener<DaqcValue> = object : UpdatableListener<DaqcValue>{
        override fun onUpdate(data: Updatable<DaqcValue>) {
            //Never Called
        }
    }

    override fun onDataUpdate(data: Updatable<DataQuantity<ElectricPotential>>) {
        //Never Called
    }
}