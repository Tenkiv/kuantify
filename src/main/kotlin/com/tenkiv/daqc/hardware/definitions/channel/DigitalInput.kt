package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.Input

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalInput: Input<DaqcValue.Boolean>, Channel<DaqcValue.Boolean> {

    abstract val canReadPulseWidthModulation: Boolean

}