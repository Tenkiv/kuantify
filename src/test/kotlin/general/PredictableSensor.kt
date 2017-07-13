package general

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.Input
import org.tenkiv.daqc.hardware.inputs.ScAnalogSensor
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
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (iteration < sendingOrder.size) {
                    broadcastChannel.offer(sendingOrder[iteration].at(Instant.now()))
                    iteration++
                } else {
                    timer.cancel()
                }

            }
        }, 100, 100)
    }
}

class PredictableDigitalSensor : Input<ValueInstant<BinaryState>> {
    override val broadcastChannel: ConflatedBroadcastChannel<ValueInstant<BinaryState>> = ConflatedBroadcastChannel()
    override val isActive: Boolean = true

    override fun activate() {}

    override fun deactivate() {}
    var iteration = 0

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
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (iteration < sendingOrder.size) {
                    broadcastChannel.offer(sendingOrder[iteration].at(Instant.now()))
                    iteration++
                } else {
                    timer.cancel()
                }

            }
        }, 100, 100)
    }

}