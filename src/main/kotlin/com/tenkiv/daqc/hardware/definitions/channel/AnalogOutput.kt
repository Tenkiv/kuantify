package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DataQuantity
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.Output
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/20/17.
 */
abstract class AnalogOutput: Output<DataQuantity<ElectricPotential>>, Channel<DataQuantity<ElectricPotential>> {

}