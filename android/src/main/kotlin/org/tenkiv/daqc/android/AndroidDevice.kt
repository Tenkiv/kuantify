package org.tenkiv.daqc.android

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class AndroidDevice(context: Context) {

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

}