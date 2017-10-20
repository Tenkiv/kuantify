package org.tenkiv.daqc.hardware.outputs

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.now
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.QuantityMeasurement
import org.tenkiv.daqc.QuantityOutput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import javax.measure.Quantity
import javax.measure.quantity.Frequency

abstract class ScDigitalFrequencyController<Q : Quantity<Q>>(val digitalOutput: DigitalOutput) :
        QuantityOutput<Q> {

    override val isActive get() = digitalOutput.isActiveForTransitionFrequency

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()

    final override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    override fun setOutput(setting: DaqcQuantity<Q>) {
        digitalOutput.sustainTransitionFrequency(convertOutput(setting))
        //TODO Change this to broadcast new setting when the board confirms the setting was received.
        _broadcastChannel.offer(setting.now())
    }

    protected abstract fun convertOutput(setting: DaqcQuantity<Q>): DaqcQuantity<Frequency>

    override fun deactivate() = digitalOutput.deactivate()
}