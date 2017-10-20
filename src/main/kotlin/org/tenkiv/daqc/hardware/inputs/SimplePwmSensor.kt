package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.QuantityInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.toDaqc
import org.tenkiv.physikal.core.hertz
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


class SimplePwmSensor internal constructor(val digitalInput: DigitalInput) : QuantityInput<Dimensionless> {

    @Volatile
    var avgFrequency: DaqcQuantity<Frequency> = 1.hertz.toDaqc()

    override val broadcastChannel get() = digitalInput.pwmBroadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForPwm

    override fun activate() = digitalInput.activateForPwm(avgFrequency)

    override fun deactivate() = digitalInput.deactivate()

}