package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.QuantityMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.at
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential


abstract class SingleChannelAnalogSensor<Q : Quantity<Q>>(analogInput: AnalogInput,
                                                          maximumEp: ComparableQuantity<ElectricPotential>) :
        Input<QuantityMeasurement<Q>> {

    override val broadcastChannel: ConflatedBroadcastChannel<QuantityMeasurement<Q>> = ConflatedBroadcastChannel()

    init {
        analogInput.setAccuracy(maxVoltage = maximumEp)
        analogInput.activate()
        analogInput.openNewCoroutineListener { measurement ->
            processNewMeasurement(convertInput(measurement.value) at measurement.instant)
        }
    }

    abstract protected fun convertInput(ep: ComparableQuantity<ElectricPotential>): DaqcQuantity<Q>

}