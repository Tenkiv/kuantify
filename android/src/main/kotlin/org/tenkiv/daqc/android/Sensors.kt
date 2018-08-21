package org.tenkiv.daqc.android

import android.hardware.Sensor
import android.hardware.SensorManager
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.android.input.AndroidSensor
import org.tenkiv.daqc.toDaqc
import org.tenkiv.physikal.core.*
import javax.measure.quantity.*

class AndroidLightSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Illuminance>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_LIGHT

    override fun convertData(data: FloatArray): DaqcQuantity<Illuminance> = data[0].lux.toDaqc()
}

class AndroidProximitySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Length>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_PROXIMITY

    override fun convertData(data: FloatArray): DaqcQuantity<Length> = data[0].centi.metre.toDaqc()
}

class AndroidPressureSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Pressure>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_PRESSURE

    override fun convertData(data: FloatArray): DaqcQuantity<Pressure> = data[0].hecto.pascal.toDaqc()
}

class AndroidRelativeHumiditySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Dimensionless>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_RELATIVE_HUMIDITY

    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

class AndroidAmbientTemperatureSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Temperature>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_AMBIENT_TEMPERATURE

    override fun convertData(data: FloatArray): DaqcQuantity<Temperature> = data[0].celsius.toDaqc()
}

class AndroidStationarySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {

    override val type: Int = Sensor.TYPE_STATIONARY_DETECT

    override fun convertData(data: FloatArray): BinaryState = BinaryState.On
}

class AndroidMotionSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {

    override val type: Int = Sensor.TYPE_MOTION_DETECT

    override fun convertData(data: FloatArray): BinaryState = BinaryState.On
}

class AndroidHeartBeatSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Dimensionless>>(manager, sensor) {

    override val type: Int = Sensor.TYPE_HEART_BEAT

    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

class AndroidOnBodySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {

    override val type: Int = Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT

    override fun convertData(data: FloatArray): BinaryState = if (data[0] == 1.0f) BinaryState.On else BinaryState.Off
}