package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.HardwareType
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalOutput: Output<DaqcValue.Boolean>, Channel<DaqcValue.Boolean> {

    override val listeners: MutableList<UpdatableListener<DaqcValue.Boolean>> = CopyOnWriteArrayList()

    abstract val canPulseWidthModulate: Boolean

    abstract fun pulsewidthModulate(dutyCycle: Float)

}