package general

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.device.Device
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency

/**
 * Created by tenkiv on 7/1/17.
 */
class EmptyAnalogInput : AnalogInput() {
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

    override val isActiveForBinaryState: Boolean
        get() = throw Exception("Empty Test Class Exception")
    override val isActiveForPwm: Boolean
        get() = throw Exception("Empty Test Class Exception")
    override val isActiveForTransitionFrequency: Boolean
        get() = throw Exception("Empty Test Class Exception")
    override val broadcastChannel: ConflatedBroadcastChannel<ValueInstant<DaqcValue>>
        get() = throw Exception("Empty Test Class Exception")

    override fun activateForTransitionFrequency() {
        throw Exception("Empty Test Class Exception")
    }

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        throw Exception("Empty Test Class Exception")
    }

    override val device: Device get() = throw Exception("Empty Test Class Exception")

    override val hardwareNumber: Int get() = throw Exception("Empty Test Class Exception")

    override fun activate() {}

    override fun deactivate() {}

}