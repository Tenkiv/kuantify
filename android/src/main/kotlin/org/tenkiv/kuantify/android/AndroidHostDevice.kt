package org.tenkiv.kuantify.android

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.provider.Settings
import org.tenkiv.kuantify.android.input.AndroidSensor
import org.tenkiv.kuantify.hardware.definitions.device.Device

class AndroidHostDevice(context: Context) : Device {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        ?: throw ClassCastException(
            "Android has somehow returned the wrong system service; this is not our problem."
        )

    val lightSensors: List<AndroidLightSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_LIGHT).map { AndroidLightSensor(sensorManager, it) }

    val hasLightSensors = lightSensors.isNotEmpty()

    val proximitySensors: List<AndroidProximitySensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PROXIMITY).map { AndroidProximitySensor(sensorManager, it) }

    val hasProximitySensors = proximitySensors.isNotEmpty()

    val pressureSensors: List<AndroidPressureSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PRESSURE).map { AndroidPressureSensor(sensorManager, it) }

    val hasPressureSensors = pressureSensors.isNotEmpty()

    val relativeHumiditySensors: List<AndroidRelativeHumiditySensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PRESSURE)
            .map { AndroidRelativeHumiditySensor(sensorManager, it) }

    val hasRelativeHumiditySensors = relativeHumiditySensors.isNotEmpty()

    val ambientTemperatureSensors: List<AndroidAmbientTemperatureSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PRESSURE)
            .map { AndroidAmbientTemperatureSensor(sensorManager, it) }

    val hasAmbientTemperatureSensors = ambientTemperatureSensors.isNotEmpty()

    val stationarySensors: List<AndroidStationarySensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PRESSURE).map { AndroidStationarySensor(sensorManager, it) }

    val hasStationarySensors = stationarySensors.isNotEmpty()

    val motionSensors: List<AndroidMotionSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PRESSURE).map { AndroidMotionSensor(sensorManager, it) }

    val hasMotionSensors = motionSensors.isNotEmpty()

    val heartBeatSensors: List<AndroidHeartBeatSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PRESSURE).map { AndroidHeartBeatSensor(sensorManager, it) }

    val hasHeartBeatSensors = heartBeatSensors.isNotEmpty()

    val onBodySensors: List<AndroidOnBodySensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PRESSURE).map { AndroidOnBodySensor(sensorManager, it) }

    val hasOnBodySensors = stationarySensors.isNotEmpty()

    /**
     * We are using a hardware ID as opposed to an installation UID as we have to try to ensure a UUID against other
     * Android devices, not just across applications. We are obviously also not using this UUID for advertising so the
     * Ad UID supplied by the system would also be incorrect. Our use case fits most similarly with telephony cases and
     * as such we use the Android ID.
     * More Info: https://developer.android.com/training/articles/user-data-ids
     */
    @SuppressLint("HardwareIds")
    override val serialNumber: String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)


    override fun initializeDevice() {
        //TODO INIT SSH server.
    }

}

/**
 * Exceptions thrown by [AndroidSensor]s when a fatal error occurs.
 */
sealed class AndroidSensorException(error: String) : Exception(error)

/**
 * Class which is thrown when a sensor is improperly initialized.
 */
class AndroidSensorInitializationException(error: String) : AndroidSensorException(error)