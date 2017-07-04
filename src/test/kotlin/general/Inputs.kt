package general

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.QuantityMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.device.Device
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency

/**
 * Created by tenkiv on 7/1/17.
 */
class EmptyAnalogInput(override val isActivated: Boolean) : AnalogInput(){
    override val device: Device get() = TODO("not implemented")
    override val hardwareNumber: Int get() = TODO("not implemented")

    override fun activate() {}

    override val broadcastChannel:
            ConflatedBroadcastChannel<QuantityMeasurement<ElectricPotential>> = ConflatedBroadcastChannel()

    override fun deactivate() {}

    override var buffer: Boolean
        get() = TODO("not implemented")
        set(value) {}
    override var maxAcceptableError: ComparableQuantity<ElectricPotential>
        get() = TODO("not implemented")
        set(value) {}
    override var maxElectricPotential: ComparableQuantity<ElectricPotential>
        get() = TODO("not implemented")
        set(value) {}
}

class EmptyDigitalInput(override val isActivated: Boolean) : DigitalInput(){
    override val broadcastChannel: ConflatedBroadcastChannel<ValueInstant<DaqcValue>>
        get() = TODO("not implemented")

    override fun activateForTransitionFrequency() {
        TODO("not implemented")
    }

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        TODO("not implemented")
    }

    override val device: Device get() = TODO("not implemented")

    override val hardwareNumber: Int get() = TODO("not implemented")

    override fun activate() {}

    override fun deactivate() {}

}