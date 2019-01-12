package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.hardware.definitions.channel.*

interface DigitalDaqDevice : SensorDevice {

    /**
     * List of all [DigitalInput]s that this device has.
     */
    val digitalInputMap: Map<String, DigitalInput>

}