package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.gate.acquire.input.QuantityInput
import org.tenkiv.kuantify.hardware.definitions.channel.AnalogInput
import tec.units.indriya.ComparableQuantity
import javax.measure.quantity.Frequency
import javax.measure.quantity.Temperature

interface AnalogDaqDevice : SensorDevice {

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
    val analogInputs: Map<Int, AnalogInput>

}