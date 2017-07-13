package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.DaqcChannel
import org.tenkiv.daqc.hardware.definitions.Updatable
import javax.measure.quantity.ElectricPotential

abstract class AnalogOutput :
        Output<DaqcQuantity<ElectricPotential>>,
        DaqcChannel,
        Updatable<DaqcQuantity<ElectricPotential>> {

}