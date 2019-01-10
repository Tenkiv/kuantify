package org.tenkiv.kuantify.android

import android.hardware.Sensor
import android.hardware.SensorManager
import org.tenkiv.kuantify.android.input.AndroidSensor
import org.tenkiv.kuantify.android.lib.beatsPerMinute
import org.tenkiv.kuantify.data.BinaryState
import org.tenkiv.kuantify.data.DaqcQuantity
import org.tenkiv.kuantify.data.toDaqc
import org.tenkiv.physikal.core.*
import javax.measure.quantity.*

/**
 * Android light sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidLightSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Illuminance>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_LIGHT

    override fun convertData(data: FloatArray): DaqcQuantity<Illuminance> = data[0].lux.toDaqc()
}

/**
 * Android proximity sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidProximitySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Length>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_PROXIMITY

    override fun convertData(data: FloatArray): DaqcQuantity<Length> = data[0].centi.metre.toDaqc()
}

/**
 * Android pressure sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidPressureSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Pressure>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_PRESSURE

    override fun convertData(data: FloatArray): DaqcQuantity<Pressure> = data[0].hecto.pascal.toDaqc()
}

/**
 * Android relative humidity sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidRelativeHumiditySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Dimensionless>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_RELATIVE_HUMIDITY

    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

/**
 * Android ambient temperature sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidAmbientTemperatureSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Temperature>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_AMBIENT_TEMPERATURE

    override fun convertData(data: FloatArray): DaqcQuantity<Temperature> = data[0].celsius.toDaqc()
}

/**
 * Android stationary sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidStationarySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {

    override val type: Int = Sensor.TYPE_STATIONARY_DETECT

    override fun convertData(data: FloatArray): BinaryState = BinaryState.On
}

/**
 * Android motion sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidMotionSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {

    override val type: Int = Sensor.TYPE_MOTION_DETECT

    override fun convertData(data: FloatArray): BinaryState = BinaryState.On
}

/**
 * Android heartbeat sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidHeartBeatSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Frequency>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_HEART_BEAT

    override fun convertData(data: FloatArray): DaqcQuantity<Frequency> = data[0].beatsPerMinute.toDaqc()
}

/**
 * Android on-body sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
class AndroidOnBodySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {

    override val type: Int = Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT

    override fun convertData(data: FloatArray): BinaryState = if (data[0] == 1.0f) BinaryState.On else BinaryState.Off
}