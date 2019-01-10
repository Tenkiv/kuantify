package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.gate.acquire.input.Input

interface SensorDevice : Device {

    val sensors: Map<Int, Input<*>>

}
