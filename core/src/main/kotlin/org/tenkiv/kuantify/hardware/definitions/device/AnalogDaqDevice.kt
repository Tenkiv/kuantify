package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.definitions.channel.*
import tec.units.indriya.*
import javax.measure.quantity.*

interface AnalogDaqDevice : Device {

    /**
     * The line frequency of the local electrical.
     */
    val lineFrequency: ComparableQuantity<Frequency>

    /**
     * The temperature reference of the board for error correction on samples.
     */
    val temperatureReference: QuantityInput<Temperature>

    /**
     * List of all [AnalogInput]s that this device has.
     */
    val analogInputMap: Map<String, AnalogInput>

}