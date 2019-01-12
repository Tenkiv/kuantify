package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.gate.acquire.input.*

interface SensorDevice : Device {

    val sensorMap: Map<String, Input<*>>

}
