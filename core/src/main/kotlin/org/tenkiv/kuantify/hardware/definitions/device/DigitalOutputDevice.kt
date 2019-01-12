package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.hardware.definitions.channel.*

interface DigitalOutputDevice : ControlDevice {

    /**
     * List of all [DigitalOutput]s that this device has.
     */
    val digitalOutputMap: Map<String, DigitalOutput>

}