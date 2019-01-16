package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.hardware.definitions.channel.*

interface DigitalDaqDevice : Device {

    /**
     * List of all [DigitalInput]s that this device has.
     */
    val digitalInputMap: Map<String, DigitalInput>

}