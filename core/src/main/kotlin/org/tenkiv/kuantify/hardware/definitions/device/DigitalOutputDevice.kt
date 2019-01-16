package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.hardware.definitions.channel.*

interface DigitalOutputDevice : Device {

    /**
     * List of all [DigitalOutput]s that this device has.
     */
    val digitalOutputMap: Map<String, DigitalOutput>

}