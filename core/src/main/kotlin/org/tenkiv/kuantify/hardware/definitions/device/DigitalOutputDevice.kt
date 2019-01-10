package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.hardware.definitions.channel.DigitalOutput

interface DigitalOutputDevice : ControlDevice {

    /**
     * List of all [DigitalOutput]s that this device has.
     */
    val digitalOutputs: Map<Int, DigitalOutput>

}