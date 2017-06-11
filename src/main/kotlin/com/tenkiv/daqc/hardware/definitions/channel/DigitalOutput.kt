package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalOutput : Output<DaqcValue.Boolean>,
        Channel<DaqcValue.Boolean>,
        Updatable<DaqcValue.Boolean> {

    abstract val canPulseWidthModulate: Boolean

    abstract fun pulseWidthModulate(dutyCycle: Int)

}