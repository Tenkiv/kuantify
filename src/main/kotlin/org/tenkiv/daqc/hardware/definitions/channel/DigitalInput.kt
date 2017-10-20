package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.BinaryStateInput
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.inputs.SimpleBinaryStateSensor
import org.tenkiv.daqc.hardware.inputs.SimpleDigitalFrequencySensor
import org.tenkiv.daqc.hardware.inputs.SimplePwmSensor
import javax.measure.quantity.Frequency

abstract class DigitalInput : DigitalChannel(), BinaryStateInput {

    override val isActive get() = super.isActive

    private val thisAsBinaryStateSensor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleBinaryStateSensor(this)
    }

    private val thisAsTransitionFrequencyInput by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleDigitalFrequencySensor(this)
    }

    private val thisAsPwmInput by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimplePwmSensor(this)
    }

    abstract fun activateForTransitionFrequency(avgFrequency: DaqcQuantity<Frequency>)

    abstract fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>)

    open fun activateForCurrentState() = activate()

    fun asBinaryStateSensor(inverted: Boolean = false) = thisAsBinaryStateSensor.apply {
        this.inverted = inverted
    }

    fun asTransitionFrequencySensor(avgFrequency: DaqcQuantity<Frequency>) = thisAsTransitionFrequencyInput.apply {
        this.avgFrequency = avgFrequency
    }

    fun asPwmSensor(avgFrequency: DaqcQuantity<Frequency>) = thisAsPwmInput.apply {
        this.avgFrequency = avgFrequency
    }
}