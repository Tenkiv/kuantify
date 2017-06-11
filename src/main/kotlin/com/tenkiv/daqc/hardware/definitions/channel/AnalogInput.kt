package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.AnalogAccuracy
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class AnalogInput:
        Input<DaqcValue.DaqcQuantity<ElectricPotential>>,
        Channel<DaqcValue.DaqcQuantity<ElectricPotential>>,
        Updatable<DaqcValue.DaqcQuantity<ElectricPotential>> {

    abstract fun setBuffer(state: Boolean)

    abstract fun setAccuracy(accuracy: AnalogAccuracy)

}