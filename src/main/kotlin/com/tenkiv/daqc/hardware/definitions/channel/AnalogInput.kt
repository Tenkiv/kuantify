package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.AnalogAccuracy
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class AnalogInput :
        Input<DaqcQuantity<ElectricPotential>>,
        Channel<DaqcQuantity<ElectricPotential>>,
        Updatable<DaqcQuantity<ElectricPotential>> {

    abstract fun setBuffer(state: Boolean)

    abstract fun setAccuracy(accuracy: AnalogAccuracy? = null,
                             maxVoltage: ComparableQuantity<ElectricPotential>)

}