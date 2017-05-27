package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.AnalogAccuracy
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.BasicUpdatable
import com.tenkiv.daqc.hardware.definitions.Channel
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class AnalogInput:
        Input<DaqcValue.Quantity<ElectricPotential>>,
        Channel<DaqcValue.Quantity<ElectricPotential>>,
        BasicUpdatable<DaqcValue.Quantity<ElectricPotential>>() {

    abstract fun setBuffer(state: Boolean)

    abstract fun setAccuracy(accuracy: AnalogAccuracy)

}