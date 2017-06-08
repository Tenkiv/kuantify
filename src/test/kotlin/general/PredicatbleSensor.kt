package general

import com.tenkiv.daqc.DaqcValue

/**
 * Created by tenkiv on 5/22/17.
 */
class PredicatbleSensor : com.tenkiv.daqc.hardware.Sensor<DaqcValue.Boolean>(emptyList<com.tenkiv.daqc.hardware.definitions.channel.Input<DaqcValue.Boolean>>()) {

    override val onDataReceived: suspend (com.tenkiv.daqc.hardware.definitions.Updatable<DaqcValue.Boolean>) -> Unit
        //Never gets data. This is a fake sensor.
        get() = { println("What on Earth happened here?") }


    var iteration = 0

    var sendingOrder = arrayListOf(
            com.tenkiv.daqc.DaqcValue.Boolean(true),
            com.tenkiv.daqc.DaqcValue.Boolean(true),
            com.tenkiv.daqc.DaqcValue.Boolean(true),
            com.tenkiv.daqc.DaqcValue.Boolean(true),
            com.tenkiv.daqc.DaqcValue.Boolean(true),
            com.tenkiv.daqc.DaqcValue.Boolean(true),
            com.tenkiv.daqc.DaqcValue.Boolean(true),
            com.tenkiv.daqc.DaqcValue.Boolean(true),
            com.tenkiv.daqc.DaqcValue.Boolean(false),
            com.tenkiv.daqc.DaqcValue.Boolean(false))

    init {
        val timer = java.util.Timer(false)

        timer.scheduleAtFixedRate(object: java.util.TimerTask() {
            override fun run() {
                latestValue = sendingOrder[iteration]
                iteration++
                if(iteration == sendingOrder.size){ this.cancel() }
            }
        },100,100)
    }

}