package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable

/**
 * Created by tenkiv on 3/18/17.
 */
interface Input<T : DaqcValue> : Updatable<T> {

    /*fun onDataUpdate(data: Updatable<T>){
        latestValue = data.latestValue
        listeners.forEach({ it.onUpdate(this) })
    }*/

}