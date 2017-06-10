package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalInput:
        Input<DaqcValue.Boolean>,
        Channel<DaqcValue.Boolean>,
        Updatable<DaqcValue.Boolean> {

    abstract val canReadPulseWidthModulation: Boolean

}