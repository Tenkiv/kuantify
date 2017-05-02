package com.tenkiv.daqc.hardware.definitions.device

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by tenkiv on 4/7/17.
 */
abstract class  DataAquisitionDevice {

    abstract fun getAnalogInputs(): List<AnalogInput>

    abstract fun getDigitalInputs(): List<DigitalInput>

    abstract fun hasAnalogInputs(): Boolean

    abstract fun hasDigitalInputs(): Boolean

    protected val sharedInputs: MutableList<Input<DaqcValue>> = CopyOnWriteArrayList()

}