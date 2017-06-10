package general

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.Sensor
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.util.*

/**
 * Created by tenkiv on 5/22/17.
 */
class PredictableSensor : Sensor<DaqcValue.Boolean>(emptyList<Input<DaqcValue.Boolean>>()) {

    suspend override fun onUpdate(updatable: Updatable<DaqcValue.Boolean>, value: DaqcValue.Boolean) {
        println("What on Earth happened here?")
    }

    var iteration = 0

    var context = newSingleThreadContext("Sensor Context")

    var sendingOrder = arrayListOf(
            DaqcValue.Boolean(true),
            DaqcValue.Boolean(true),
            DaqcValue.Boolean(true),
            DaqcValue.Boolean(true),
            DaqcValue.Boolean(true),
            DaqcValue.Boolean(true),
            DaqcValue.Boolean(true),
            DaqcValue.Boolean(true),
            DaqcValue.Boolean(false),
            DaqcValue.Boolean(false))

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