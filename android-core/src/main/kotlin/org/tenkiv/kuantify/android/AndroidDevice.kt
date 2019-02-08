package org.tenkiv.kuantify.android

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.quantity.*

interface AndroidDevice : FSDevice {

    val ambientTemperatureSensors: List<QuantityAndroidSensor<Temperature>>

    val heartRateSensors: List<QuantityAndroidSensor<Frequency>>

    val lightSensors: List<QuantityAndroidSensor<Illuminance>>

    val proximitySensors: List<QuantityAndroidSensor<Length>>

    val pressureSensors: List<QuantityAndroidSensor<Pressure>>

    val relativeHumiditySensors: List<QuantityAndroidSensor<Dimensionless>>

    @Serializable
    data class Info(
        val deviceId: String,
        val numAmbientTemperatureSensors: Int,
        val numHeartRateSensors: Int,
        val numLightSensors: Int,
        val numProximitySensors: Int,
        val numPressureSensors: Int,
        val numRelativeHumiditySensors: Int
    )
}

class RemoteAndroidDevice internal constructor(
    scope: CoroutineScope,
    override val hostIp: String,
    override val uid: String,
    override val ambientTemperatureSensors: List<RemoteQuantityAndroidSensor<Temperature>>,
    override val heartRateSensors: List<RemoteQuantityAndroidSensor<Frequency>>,
    override val lightSensors: List<RemoteQuantityAndroidSensor<Illuminance>>,
    override val pressureSensors: List<RemoteQuantityAndroidSensor<Pressure>>,
    override val proximitySensors: List<RemoteQuantityAndroidSensor<Length>>,
    override val relativeHumiditySensors: List<RemoteQuantityAndroidSensor<Dimensionless>>
) : FSRemoteDevice(scope), AndroidDevice {

    override fun sideConfig(config: SideRouteConfig) {
        super.sideConfig(config)
        ambientTemperatureSensors.applySideConfigsTo(config)
        heartRateSensors.applySideConfigsTo(config)
        lightSensors.applySideConfigsTo(config)
        pressureSensors.applySideConfigsTo(config)
        proximitySensors.applySideConfigsTo(config)
        relativeHumiditySensors.applySideConfigsTo(config)
    }
}

//TODO: Error handling
suspend fun CoroutineScope.RemoteAndroidDeivce(hostIp: String): RemoteAndroidDevice {
    val info = Json.parse(AndroidDevice.Info.serializer(), FSRemoteDevice.getInfo(hostIp))

    val ambientTemperatureSensors = ArrayList<RemoteQuantityAndroidSensor<Temperature>>()
    for (i in 0..info.numAmbientTemperatureSensors) {
        ambientTemperatureSensors += RemoteQuantityAndroidSensor(
            this,
            "${AndroidSensorTypeId.AMBIENT_TEMPERATURE}$i",
            Temperature::class
        )
    }

    val heartRateSensors = ArrayList<RemoteQuantityAndroidSensor<Frequency>>()
    for (i in 0..info.numHeartRateSensors) {
        heartRateSensors += RemoteQuantityAndroidSensor(
            this,
            "${AndroidSensorTypeId.AMBIENT_TEMPERATURE}$i",
            Frequency::class
        )
    }

    val lightSensors = ArrayList<RemoteQuantityAndroidSensor<Illuminance>>()
    for (i in 0..info.numHeartRateSensors) {
        lightSensors += RemoteQuantityAndroidSensor(
            this,
            "${AndroidSensorTypeId.LIGHT}$i",
            Illuminance::class
        )
    }

    val pressureSensors = ArrayList<RemoteQuantityAndroidSensor<Pressure>>()
    for (i in 0..info.numPressureSensors) {
        pressureSensors += RemoteQuantityAndroidSensor(
            this,
            "${AndroidSensorTypeId.PRESSURE}$i",
            Pressure::class
        )
    }

    val proximitySensors = ArrayList<RemoteQuantityAndroidSensor<Length>>()
    for (i in 0..info.numPressureSensors) {
        proximitySensors += RemoteQuantityAndroidSensor(
            this,
            "${AndroidSensorTypeId.PROXIMITY}$i",
            Length::class
        )
    }

    val relativeHumiditySensors = ArrayList<RemoteQuantityAndroidSensor<Dimensionless>>()
    for (i in 0..info.numPressureSensors) {
        relativeHumiditySensors += RemoteQuantityAndroidSensor(
            this,
            "${AndroidSensorTypeId.RELATIVE_HUMIDITY}$i",
            Dimensionless::class
        )
    }

    return RemoteAndroidDevice(
        this,
        hostIp,
        info.deviceId,
        ambientTemperatureSensors,
        heartRateSensors,
        lightSensors,
        pressureSensors,
        proximitySensors,
        relativeHumiditySensors
    )
}