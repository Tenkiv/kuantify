package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.device.Device
import tec.units.indriya.ComparableQuantity
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency

/**
 * Created by tenkiv on 7/1/17.
 */
class EmptyAnalogInput : AnalogInput() {
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val sampleRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override val device: Device
        get() = throw Exception("Empty Test Class Exception")
    override val hardwareNumber: Int
        get() = throw Exception("Empty Test Class Exception")
    override val isActive: Boolean
        get() = throw Exception("Empty Test Class Exception")

    override fun activate() {}

    override val broadcastChannel:
            ConflatedBroadcastChannel<QuantityMeasurement<ElectricPotential>> = ConflatedBroadcastChannel()

    override fun deactivate() {}

    override var buffer: Boolean
        get() = throw Exception("Empty Test Class Exception")
        set(value) {}
    override var maxAcceptableError: ComparableQuantity<ElectricPotential>
        get() = throw Exception("Empty Test Class Exception")
        set(value) {}
    override var maxElectricPotential: ComparableQuantity<ElectricPotential>
        get() = throw Exception("Empty Test Class Exception")
        set(value) {}
}

class EmptyDigitalInput : DigitalInput() {
    override val sampleRate: ComparableQuantity<Frequency>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun activateForTransitionFrequency(avgFrequency: DaqcQuantity<Frequency>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val transitionFrequencyIsSimulated: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val pwmIsSimulated: Boolean
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val isActiveForBinaryState: Boolean
        get() = throw Exception("Empty Test Class Exception")
    override val isActiveForPwm: Boolean
        get() = throw Exception("Empty Test Class Exception")
    override val isActiveForTransitionFrequency: Boolean
        get() = throw Exception("Empty Test Class Exception")

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        throw Exception("Empty Test Class Exception")
    }

    override val device: Device get() = throw Exception("Empty Test Class Exception")

    override val hardwareNumber: Int get() = throw Exception("Empty Test Class Exception")

    override fun activate() {}

    override fun deactivate() {}

}