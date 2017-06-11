package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable

/**
 * Created by tenkiv on 3/18/17.
 */
interface Output<T : DaqcValue> : Updatable<T> {

    /*fun setState(state: T){
        latestValue = state
    }*/
}