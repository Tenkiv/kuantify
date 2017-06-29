package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.hardware.definitions.Channel

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalOutput : Output<BinaryState>,
        Channel<BinaryState> {

    abstract val canPulseWidthModulate: Boolean

    abstract fun pulseWidthModulate(dutyCycle: Int)

}