package org.tenkiv.kuantify.android

import android.hardware.*
import org.tenkiv.kuantify.android.input.*
import org.tenkiv.kuantify.android.lib.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.physikal.core.*
import javax.measure.quantity.*

/**
 * Android light sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidLightSensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Illuminance>>(device, sensor) {

    override val type: Int = Sensor.TYPE_LIGHT

    override fun convertData(data: FloatArray): DaqcQuantity<Illuminance> = data[0].lux.toDaqc()
}

/**
 * Android proximity sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidProximitySensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Length>>(device, sensor) {

    override val type: Int = Sensor.TYPE_PROXIMITY

    override fun convertData(data: FloatArray): DaqcQuantity<Length> = data[0].centi.metre.toDaqc()
}

/**
 * Android pressure sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidPressureSensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Pressure>>(device, sensor) {

    override val type: Int = Sensor.TYPE_PRESSURE

    override fun convertData(data: FloatArray): DaqcQuantity<Pressure> = data[0].hecto.pascal.toDaqc()
}

/**
 * Android relative humidity sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidRelativeHumiditySensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Dimensionless>>(device, sensor) {

    override val type: Int = Sensor.TYPE_RELATIVE_HUMIDITY

    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

/**
 * Android ambient temperature sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidAmbientTemperatureSensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Temperature>>(device, sensor) {

    override val type: Int = Sensor.TYPE_AMBIENT_TEMPERATURE

    override fun convertData(data: FloatArray): DaqcQuantity<Temperature> = data[0].celsius.toDaqc()
}

/**
 * Android stationary sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidStationarySensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<BinaryState>(device, sensor) {

    override val type: Int = Sensor.TYPE_STATIONARY_DETECT

    override fun convertData(data: FloatArray): BinaryState = BinaryState.High
}

/**
 * Android motion sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidMotionSensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<BinaryState>(device, sensor) {

    override val type: Int = Sensor.TYPE_MOTION_DETECT

    override fun convertData(data: FloatArray): BinaryState = BinaryState.High
}

/**
 * Android heartbeat sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidHeartBeatSensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Frequency>>(device, sensor) {

    override val type: Int = Sensor.TYPE_HEART_BEAT

    override fun convertData(data: FloatArray): DaqcQuantity<Frequency> = data[0].beatsPerMinute.toDaqc()
}

/**
 * Android on-body sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidOnBodySensor(device: LocalAndroidDevice, sensor: Sensor) :
    AndroidSensor<BinaryState>(device, sensor) {

    override val type: Int = Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT

    override fun convertData(data: FloatArray): BinaryState = if (data[0] == 1.0f) BinaryState.High else BinaryState.Low
}