package org.tenkiv.daqc.hardware.outputs

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.now
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityOutput
import javax.measure.Quantity
import javax.measure.quantity.Dimensionless

abstract class ScPwmController<Q : Quantity<Q>>(val digitalOutput: DigitalOutput) :
        QuantityOutput<Q> {

    override val isActive get() = digitalOutput.isActiveForPwm

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    override fun setOutput(setting: DaqcQuantity<Q>) {
        digitalOutput.pulseWidthModulate(convertOutput(setting))
        //TODO Change this to broadcast new setting when the board confirms the setting was received.
        _broadcastChannel.offer(setting.now())
    }

    protected abstract fun convertOutput(setting: DaqcQuantity<Q>): DaqcQuantity<Dimensionless>

    override fun deactivate() = digitalOutput.deactivate()
}