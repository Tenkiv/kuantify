package org.tenkiv.kuantify.android.input

import android.hardware.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.android.lib.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.physikal.core.*
import javax.measure.quantity.*

/**
 * Android ambient temperature sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class LocalAndroidAmbientTemperatureSensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Temperature>>(device, sensor, uid) {

    override val type: Int get() = Sensor.TYPE_AMBIENT_TEMPERATURE

    override fun convertData(data: FloatArray): DaqcQuantity<Temperature> = data[0].celsius.toDaqc()
}

class LocalAndroidHeartRateSensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Frequency>>(device, sensor, uid) {
    override val type: Int get() = Sensor.TYPE_HEART_RATE

    override fun convertData(data: FloatArray): DaqcQuantity<Frequency> = data[0].beatsPerMinute.toDaqc()
}

/**
 * Android light sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class LocalAndroidLightSensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Illuminance>>(device, sensor, uid) {

    override val type: Int get() = Sensor.TYPE_LIGHT

    override fun convertData(data: FloatArray): DaqcQuantity<Illuminance> = data[0].lux.toDaqc()
}

/**
 * Android proximity sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class LocalAndroidProximitySensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Length>>(device, sensor, uid) {

    override val type: Int get() = Sensor.TYPE_PROXIMITY

    override fun convertData(data: FloatArray): DaqcQuantity<Length> = data[0].centi.metre.toDaqc()
}

/**
 * Android pressure sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class LocalAndroidPressureSensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Pressure>>(device, sensor, uid) {

    override val type: Int get() = Sensor.TYPE_PRESSURE

    override fun convertData(data: FloatArray): DaqcQuantity<Pressure> = data[0].hecto.pascal.toDaqc()
}

/**
 * Android relative humidity sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class LocalAndroidRelativeHumiditySensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Dimensionless>>(device, sensor, uid) {

    override val type: Int get() = Sensor.TYPE_RELATIVE_HUMIDITY

    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

object AndroidSensorTypeId {
    const val AMBIENT_TEMPERATURE = "AT"
    const val HEART_RATE = "HR"
    const val LIGHT = "LI"
    const val PROXIMITY = "PX"
    const val PRESSURE = "PS"
    const val RELATIVE_HUMIDITY = "HU"
}