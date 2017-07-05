package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqc.hardware.definitions.device.Device

interface DaqcChannel {

    val device: Device

    val hardwareNumber: Int

}