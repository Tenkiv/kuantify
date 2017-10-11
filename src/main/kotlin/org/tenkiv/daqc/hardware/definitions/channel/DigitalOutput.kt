package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.BinaryStateOutput
import org.tenkiv.daqc.hardware.outputs.SimpleBinaryStateController
import org.tenkiv.daqc.hardware.outputs.SimpleFrequencyController
import org.tenkiv.daqc.hardware.outputs.SimplePwmController
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency


abstract class DigitalOutput : DigitalChannel(), BinaryStateOutput {

    override val isActive get() = super.isActive

    private val thisAsBinaryStateController by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleBinaryStateController(this)
    }

    private val thisAsPwmController by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimplePwmController(this)
    }

    private val thisAsFrequencyController by lazy(LazyThreadSafetyMode.PUBLICATION) {
        SimpleFrequencyController(this)
    }

    abstract fun pulseWidthModulate(percent: DaqcQuantity<Dimensionless>)

    abstract fun sustainTransitionFrequency(freq: DaqcQuantity<Frequency>)

    override fun deactivate() = setOutput(BinaryState.Off)

    fun asBinaryStateController(inverted: Boolean = false) = thisAsBinaryStateController.apply {
        this.inverted = inverted
    }

    fun asPwmController() = thisAsPwmController

    fun asFrequencyController() = thisAsFrequencyController


}