package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.Input
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class AnalogInput: Input<DaqcValue.Quantity<ElectricPotential>>, Channel<DaqcValue.Quantity<ElectricPotential>> {

    abstract fun setRate()

    abstract fun setGain()

    abstract fun setBuffer()

    abstract fun setAccuracy()


}