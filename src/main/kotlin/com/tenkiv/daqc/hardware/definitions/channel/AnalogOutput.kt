package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.definitions.DaqcChannel
import com.tenkiv.daqc.hardware.definitions.Updatable
import javax.measure.quantity.ElectricPotential

abstract class AnalogOutput :
        Output<DaqcQuantity<ElectricPotential>>,
        DaqcChannel,
        Updatable<DaqcQuantity<ElectricPotential>> {

}