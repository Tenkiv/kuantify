package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.hardware.definitions.DaqcChannel
import javax.measure.quantity.ElectricPotential

abstract class AnalogOutput :
        QuantityOutput<ElectricPotential>,
        DaqcChannel {

}