package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Temperature

/**
 * Created by tenkiv on 4/17/17.
 */
class ThermocoupleK: Sensor<DaqcValue>(emptyList()) {

    override val onDataReceived: UpdatableListener<DaqcValue> = object: UpdatableListener<DaqcValue>{
        override fun onUpdate(updatedObject: Updatable<DaqcValue>) {

        }
    }
}