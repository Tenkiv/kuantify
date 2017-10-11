package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.hardware.definitions.device.Device

interface DaqcChannel {

    val device: Device

    val hardwareNumber: Int

}