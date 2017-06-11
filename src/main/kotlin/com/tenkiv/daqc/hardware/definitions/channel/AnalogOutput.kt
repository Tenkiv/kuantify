package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/20/17.
 */
abstract class AnalogOutput :
        Output<DaqcQuantity<ElectricPotential>>,
        Channel<DaqcQuantity<ElectricPotential>>,
        Updatable<DaqcQuantity<ElectricPotential>> {

}