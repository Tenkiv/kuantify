package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.device.Device
import org.tenkiv.physikal.core.hertz
import org.tenkiv.physikal.core.percent
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.time.Instant
import java.util.*
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency


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

class DigitalInputGibberingSensor : DigitalInput() {
    override fun activateForTransitionFrequency(avgFrequency: DaqcQuantity<Frequency>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val isActiveForBinaryState: Boolean = false
    override val isActiveForPwm: Boolean = false
    override val isActiveForTransitionFrequency: Boolean = false
    override val pwmIsSimulated: Boolean = false
    override val transitionFrequencyIsSimulated: Boolean = false
    override val device: Device
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val hardwareNumber: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                _binaryStateBroadcastChannel.offer(BinaryState.On.at(Instant.now()))
                _transitionFrequencyBroadcastChannel.offer(DaqcQuantity.of(10.hertz).at(Instant.now()))
                _pwmBroadcastChannel.offer(DaqcQuantity.of(10.percent).at(Instant.now()))
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

class AnalogInputGibberingSensor : AnalogInput() {
    override var buffer: Boolean = false
    override val sampleRate: ComparableQuantity<Frequency>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override var maxAcceptableError: ComparableQuantity<ElectricPotential>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override var maxElectricPotential: ComparableQuantity<ElectricPotential>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override val device: Device
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val hardwareNumber: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
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
                        DaqcQuantity.of(
                                random.nextInt(5000),
                                MetricPrefix.MILLI(Units.VOLT)).at(Instant.now()))
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