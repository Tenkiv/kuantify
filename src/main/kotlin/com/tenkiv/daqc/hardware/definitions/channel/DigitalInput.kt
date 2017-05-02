package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.OnOff
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.Input

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalInput: Input<OnOff>, Channel<OnOff> {

    abstract val canReadPulseWidthModulation: Boolean

}