package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device

/**
 * Created by tenkiv on 3/18/17.
 */
interface Output<T: DaqcValue>: Updatable<T> {

    fun setState(state: T){
        value = state
    }
}