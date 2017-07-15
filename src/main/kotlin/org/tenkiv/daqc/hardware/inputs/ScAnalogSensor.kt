package org.tenkiv.daqc.hardware.inputs

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.QuantityMeasurement
import org.tenkiv.coral.at
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.Input
import org.tenkiv.daqc.lib.ValueOutOfRangeException
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
            try {
                sendNewMeasurement(convertInput(measurement.value) at measurement.instant)
            } catch (e: ValueOutOfRangeException) {
                System.err.println("Voltage out of acceptable range for sensor:\n" +
                        " $this \n" +
                        "${e.printStackTrace()}")
            }
        }
    }

    abstract protected fun convertInput(ep: ComparableQuantity<ElectricPotential>): DaqcQuantity<Q>

    override fun activate() = analogInput.activate()

    override fun deactivate() = analogInput.deactivate()
}