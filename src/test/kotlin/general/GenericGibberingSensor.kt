package general

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.QuantityMeasurement
import com.tenkiv.daqc.hardware.SingleChannelAnalogSensor
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.hardware.definitions.device.Device
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.at
import org.tenkiv.physikal.core.micro
import org.tenkiv.physikal.core.milli
import org.tenkiv.physikal.core.volt
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.time.Instant
import java.util.*
import javax.measure.quantity.ElectricPotential



class GenericGibberingSensor : SingleChannelAnalogSensor<ElectricPotential>(EmptyAnalogInput(),3.micro.volt,3.micro.volt) {

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
        SingleChannelAnalogSensor<ElectricPotential>(EmptyAnalogInput(),3.micro.volt,3.micro.volt) {

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                launch(DAQC_CONTEXT) {
                    broadcastChannel.send(
                            DaqcQuantity.of(random.nextInt(5000), MetricPrefix.MILLI(Units.VOLT)).at(Instant.now()))
                }
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