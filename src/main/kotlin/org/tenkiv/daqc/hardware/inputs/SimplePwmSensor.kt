package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.QuantityMeasurement
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.Input
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


class SimplePwmSensor(val digitalInput: DigitalInput,
                      val avgFrequency: DaqcQuantity<Frequency>) : Input<QuantityMeasurement<Dimensionless>> {

    override val broadcastChannel get() = digitalInput.pwmBroadcastChannel

    override val isActive get() = digitalInput.isActiveForPwm

    override fun activate() = digitalInput.activateForPwm(avgFrequency)

    override fun deactivate() = digitalInput.deactivate()

}