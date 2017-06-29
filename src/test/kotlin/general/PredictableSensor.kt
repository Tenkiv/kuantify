package general

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.SingleChannelAnalogSensor
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.physikal.core.volt
import java.util.*
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 5/22/17.
 */
class PredictableDigitalSensor : SingleChannelAnalogSensor<BinaryState>(emptyList<Input<BinaryState>>()) {

    suspend override fun onUpdate(updatable: Updatable<BinaryState>, value: BinaryState) {
        println("What on Earth happened here?")
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
                        broadcastChannel.send(sendingOrder[iteration])
                        iteration++
                    } else {
                        timer.cancel()
                    }
                }
            }
        },100,100)
    }

}

class PredictableAnalogSensor : SingleChannelAnalogSensor<DaqcQuantity<ElectricPotential>>(emptyList<Input<DaqcQuantity<ElectricPotential>>>()) {

    suspend override fun onUpdate(updatable: Updatable<DaqcQuantity<ElectricPotential>>, value: DaqcQuantity<ElectricPotential>) {
        println("What on Earth happened here?")
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
                        broadcastChannel.send(sendingOrder[iteration])
                        iteration++
                    } else {
                        timer.cancel()
                    }
                }
            }
        },100,100)
    }

}