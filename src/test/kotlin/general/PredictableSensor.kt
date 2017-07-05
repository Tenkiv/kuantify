package general

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.ScAnalogSensor
import com.tenkiv.daqc.hardware.SimpleBinaryStateSensor
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.physikal.core.milli
import org.tenkiv.physikal.core.volt
import tec.uom.se.ComparableQuantity
import java.time.Instant
import java.util.*
import javax.measure.quantity.ElectricPotential

class PredictableAnalogSensor : ScAnalogSensor<ElectricPotential>(EmptyAnalogInput(), 3.volt, 3.milli.volt) {
    override fun activate() {}

    override fun deactivate() {}

    override fun convertInput(ep: ComparableQuantity<ElectricPotential>): DaqcQuantity<ElectricPotential> {
        return DaqcQuantity.Companion.of(2000.volt)
    }

    var iteration = 0

    var context = newSingleThreadContext("Sensor Context")

    var sendingOrder = arrayListOf(
            DaqcQuantity.of(2.volt),
            DaqcQuantity.of(4.volt),
            DaqcQuantity.of(6.volt),
            DaqcQuantity.of(8.volt),
            DaqcQuantity.of(10.volt),
            DaqcQuantity.of(12.volt),
            DaqcQuantity.of(14.volt),
            DaqcQuantity.of(16.volt),
            DaqcQuantity.of(18.volt),
            DaqcQuantity.of(20.volt))

    init {
        val timer = Timer(false)
        // Just a very hacky way of simulating an input. Needs to be thread safe to be predictable.
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                launch(context) {
                    if (iteration < sendingOrder.size) {
                        broadcastChannel.send(sendingOrder[iteration].at(Instant.now()))
                        iteration++
                    } else {
                        timer.cancel()
                    }
                }
            }
        },100,100)
    }
}

class PredictableDigitalSensor: Input<ValueInstant<BinaryState>>{
    override val broadcastChannel: ConflatedBroadcastChannel<ValueInstant<BinaryState>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val isActive: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun activate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deactivate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    var iteration = 0

    var context = newSingleThreadContext("Sensor Context")

    var sendingOrder = arrayListOf(
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.Off,
            BinaryState.Off)

    init {
        val timer = Timer(false)
        // Just a very hacky way of simulating an input. Needs to be thread safe to be predictable.
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                launch(context) {
                    if (iteration < sendingOrder.size) {
                        broadcastChannel.send(sendingOrder[iteration].at(Instant.now()))
                        iteration++
                    } else {
                        timer.cancel()
                    }
                }
            }
        },100,100)
    }

}