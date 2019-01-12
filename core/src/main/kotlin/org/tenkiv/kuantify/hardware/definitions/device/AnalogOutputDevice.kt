package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.hardware.definitions.channel.*

interface AnalogOutputDevice : ControlDevice {

    /**
     * List of all [AnalogOutput]s that this device has.
     */
    val analogOutputMap: Map<String, AnalogOutput>

}