package org.tenkiv.daqc.android.input

import android.hardware.Sensor
import android.hardware.SensorManager
import org.tenkiv.daqc.*
import org.tenkiv.daqc.android.input.AndroidSensor
import org.tenkiv.physikal.core.*
import javax.measure.quantity.*

class AndroidLightSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Illuminance>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Illuminance> = data[0].lux.toDaqc()
}

class AndroidProximitySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Length>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Length> = data[0].centi.metre.toDaqc()
}

class AndroidPressureSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Pressure>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Pressure> = data[0].hecto.pascal.toDaqc()
}

class AndroidRelativeHumiditySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Dimensionless>>(manager, sensor), RangedInput<DaqcQuantity<Dimensionless>> {
    override val valueRange: ClosedRange<DaqcQuantity<Dimensionless>> = 0.percent.toDaqc()..100.percent.toDaqc()

    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

class AndroidAmbientTemperatureSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Temperature>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Temperature> = data[0].celsius.toDaqc()
}

class AndroidStationarySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {
    override fun convertData(data: FloatArray): BinaryState = BinaryState.On
}

class AndroidMotionSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {
    override fun convertData(data: FloatArray): BinaryState = BinaryState.On
}

class AndroidHeartBeatSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Dimensionless>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

class AndroidOnBodySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor), BinaryStateInput {
    override fun convertData(data: FloatArray): BinaryState = if (data[0] == 1.0f) BinaryState.On else BinaryState.Off
}