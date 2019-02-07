package org.tenkiv.kuantify.android

import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import javax.measure.quantity.*

interface AndroidDevice : FSDevice {

    val ambientTemperatureSensors: List<QuantityInput<Temperature>>

    val heartRateSensors: List<QuantityInput<Frequency>>

    val lightSensors: List<QuantityInput<Illuminance>>

    val proximitySensors: List<QuantityInput<Length>>

    val pressureSensors: List<QuantityInput<Pressure>>

    val relativeHumiditySensors: List<QuantityInput<Dimensionless>>

}