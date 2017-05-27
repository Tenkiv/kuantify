package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.BasicUpdatable
import com.tenkiv.daqc.hardware.definitions.Channel
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/20/17.
 */
abstract class AnalogOutput:
        Output<DaqcValue.Quantity<ElectricPotential>>,
        Channel<DaqcValue.Quantity<ElectricPotential>>,
        BasicUpdatable<DaqcValue.Quantity<ElectricPotential>>() {

}