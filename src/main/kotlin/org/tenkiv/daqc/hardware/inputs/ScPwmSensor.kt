package org.tenkiv.daqc.hardware.inputs

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.at
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityInput
import org.tenkiv.daqc.lib.openNewCoroutineListener
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

abstract class ScPwmSensor<Q : Quantity<Q>>(val digitalInput: DigitalInput,
                                            val avgFrequency: DaqcQuantity<Frequency>) :
        QuantityInput<Q> {

    override val broadcastChannel: ConflatedBroadcastChannel<QuantityMeasurement<Q>> = ConflatedBroadcastChannel()

    override val isActive get() = digitalInput.isActiveForPwm

    init {
        digitalInput.pwmBroadcastChannel.openNewCoroutineListener(CommonPool) {
            sendNewMeasurement(convertInput(it.value) at it.instant)
        }
    }

    override fun activate() = digitalInput.activateForPwm(avgFrequency)

    override fun deactivate() = digitalInput.deactivate()

    protected abstract fun convertInput(percentOn: ComparableQuantity<Dimensionless>): DaqcQuantity<Q>
}