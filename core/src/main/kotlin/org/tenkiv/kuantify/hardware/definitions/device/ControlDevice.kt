package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.gate.control.output.Output

interface ControlDevice : Device {

    val controllers: Map<Int, Output<*>>

}