package com.tenkiv.daqc.hardware.inputs

import com.tenkiv.QuantityMeasurement
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


class SimplePwmSensor(val digitalInput: DigitalInput,
                      val avgFrequency: DaqcQuantity<Frequency>) : Input<QuantityMeasurement<Dimensionless>> {

    override val broadcastChannel get() = digitalInput.pwmBroadcastChannel

    override val isActive get() = digitalInput.isActiveForPwm

    override fun activate() = digitalInput.activateForPwm(avgFrequency)

    override fun deactivate() = digitalInput.deactivate()

}