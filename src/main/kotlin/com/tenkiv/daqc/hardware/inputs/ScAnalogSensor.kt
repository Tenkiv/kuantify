package com.tenkiv.daqc.hardware.inputs

import com.tenkiv.QuantityMeasurement
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.at
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential


abstract class ScAnalogSensor<Q : Quantity<Q>>(
        val analogInput: AnalogInput,
        maximumEp: ComparableQuantity<ElectricPotential>,
        acceptableError: ComparableQuantity<ElectricPotential>
) : Input<QuantityMeasurement<Q>> {

    override val broadcastChannel: ConflatedBroadcastChannel<QuantityMeasurement<Q>> = ConflatedBroadcastChannel()

    override val isActive get() = analogInput.isActive

    init {
        analogInput.maxElectricPotential = maximumEp
        analogInput.maxAcceptableError = acceptableError
        analogInput.openNewCoroutineListener(CommonPool) { measurement ->
            processNewMeasurement(convertInput(measurement.value) at measurement.instant)
        }
    }

    abstract protected fun convertInput(ep: ComparableQuantity<ElectricPotential>): DaqcQuantity<Q>

    override fun activate() = analogInput.activate()

    override fun deactivate() = analogInput.deactivate()
}