package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.device.Device

/**
 * Created by tenkiv on 3/18/17.
 */
interface DaqcChannel<T : DaqcValue> {

    val device: Device

    val hardwareNumber: Int

}