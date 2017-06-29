package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import org.tenkiv.coral.ValueInstant

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalInput :
        Input<ValueInstant<DaqcValue>>,
        Channel<DaqcValue> {

    abstract fun activateForTransitionCount()

    abstract fun activateForPercentageOn()

    open fun activateForCurrentState(){ activate() }
}