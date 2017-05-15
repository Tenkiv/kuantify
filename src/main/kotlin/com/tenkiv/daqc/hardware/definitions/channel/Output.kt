package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import tec.uom.se.unit.Units
import javax.measure.Quantity
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Time

/**
 * Created by tenkiv on 3/18/17.
 */
interface Output<T: DaqcValue>: Updatable<T> {

    /*fun setState(state: T){
        value = state
    }*/
}