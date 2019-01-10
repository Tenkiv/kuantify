package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.hardware.definitions.channel.AnalogOutput

interface AnalogOutputDevice : ControlDevice {

    /**
     * List of all [AnalogOutput]s that this device has.
     */
    val analogOutputs: Map<Int, AnalogOutput>

}