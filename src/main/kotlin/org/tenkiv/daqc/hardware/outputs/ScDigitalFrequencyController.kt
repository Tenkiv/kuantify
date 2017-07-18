package org.tenkiv.daqc.hardware.outputs

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.now
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.hardware.definitions.channel.StandardQuantityOutput
import javax.measure.Quantity
import javax.measure.quantity.Frequency

abstract class ScDigitalFrequencyController<Q : Quantity<Q>>(val digitalOutput: DigitalOutput) :
        StandardQuantityOutput<Q> {

    override val isActive get() = digitalOutput.isActiveForTransitionFrequency

    override val broadcastChannel: ConflatedBroadcastChannel<QuantityMeasurement<Q>> = ConflatedBroadcastChannel()

    override fun setOutput(setting: DaqcQuantity<Q>) {
        digitalOutput.sustainTransitionFrequency(convertOutput(setting))
        //TODO Change this to broadcast new setting when the board confirms the setting was received.
        broadcastChannel.offer(setting.now())
    }

    abstract fun convertOutput(setting: DaqcQuantity<Q>): DaqcQuantity<Frequency>

    override fun deactivate() = digitalOutput.deactivate()
}