package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.hardware.definitions.channel.DigitalInput

interface DigitalDaqDevice : SensorDevice {

    /**
     * List of all [DigitalInput]s that this device has.
     */
    val digitalInputs: Map<Int, DigitalInput>

}