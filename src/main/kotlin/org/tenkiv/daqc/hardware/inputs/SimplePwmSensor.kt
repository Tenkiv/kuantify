package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityInput
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


class SimplePwmSensor(val digitalInput: DigitalInput,
                      val avgFrequency: DaqcQuantity<Frequency>) : QuantityInput<Dimensionless> {

    override val broadcastChannel get() = digitalInput.pwmBroadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForPwm

    override fun activate() = digitalInput.activateForPwm(avgFrequency)

    override fun deactivate() = digitalInput.deactivate()

}