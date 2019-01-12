package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.gate.control.output.*

interface ControlDevice : Device {

    val controllerMap: Map<String, Output<*>>

}