package general

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.Input
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.time.Instant
import java.util.*
import javax.measure.quantity.ElectricPotential


class DigitalGibberingSensor : Input<BinaryState> {
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<BinaryState>>()
    override val isActive: Boolean = false

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                broadcastChannel.offer(BinaryState.On.at(Instant.now()))
            }
        }, 100, 100)
    }

    override fun activate() {}

    override fun deactivate() {}

    fun cancel() {
        timer.cancel()
        broadcastChannel.close()
    }
}

class AnalogGibberingSensor : Input<DaqcQuantity<ElectricPotential>> {
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<ElectricPotential>>>()

    override val isActive: Boolean = false

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                broadcastChannel.offer(
                        DaqcQuantity.of(random.nextInt(5000), MetricPrefix.MILLI(Units.VOLT)).at(Instant.now()))

            }
        }, 100, 100)
    }

    override fun activate() {}

    override fun deactivate() {}

    fun cancel() {
        timer.cancel()
        broadcastChannel.close()
    }
}