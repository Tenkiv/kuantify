package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity

/**
 * Created by tenkiv on 3/18/17.
 */
interface Input<T: DaqcValue>: Updatable<T>{

    /*fun onDataUpdate(data: Updatable<T>){
        value = data.value
        listeners.forEach({ it.onUpdate(this) })
    }*/

}