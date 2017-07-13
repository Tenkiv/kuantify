package org.tenkiv.daqc.hardware.outputs

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.hardware.definitions.channel.Output
import javax.measure.Quantity
import javax.measure.quantity.Dimensionless

abstract class ScPwmController<Q : Quantity<Q>>(val digitalOutput: DigitalOutput) :
        Output<DaqcQuantity<Q>>, Updatable<DaqcQuantity<Q>> {

    override val isActive get() = digitalOutput.isActiveForPwm

    override val broadcastChannel: ConflatedBroadcastChannel<DaqcQuantity<Q>> = ConflatedBroadcastChannel()

    override fun setOutput(setting: DaqcQuantity<Q>) {
        digitalOutput.pulseWidthModulate(convertOutput(setting))
        broadcastChannel.offer(setting)
    }

    abstract fun convertOutput(setting: DaqcQuantity<Q>): DaqcQuantity<Dimensionless>

    override fun deactivate() = digitalOutput.deactivate()
}