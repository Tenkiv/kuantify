/*
 * Copyright 2018 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.daqc.android.input

import android.hardware.Sensor
import android.hardware.SensorManager
import org.tenkiv.daqc.*
import org.tenkiv.daqc.android.input.AndroidSensor
import org.tenkiv.physikal.core.*
import javax.measure.quantity.*

/**
 * Android light sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidLightSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Illuminance>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Illuminance> = data[0].lux.toDaqc()
}

/**
 * Android proximity sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidProximitySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Length>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Length> = data[0].centi.metre.toDaqc()
}

/**
 * Android pressure sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidPressureSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Pressure>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Pressure> = data[0].hecto.pascal.toDaqc()
}

/**
 * Android relative humidity sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidRelativeHumiditySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Dimensionless>>(manager, sensor), RangedInput<DaqcQuantity<Dimensionless>> {
    override val valueRange: ClosedRange<DaqcQuantity<Dimensionless>> = 0.percent.toDaqc()..100.percent.toDaqc()

    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

/**
 * Android ambient temperature sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidAmbientTemperatureSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Temperature>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Temperature> = data[0].celsius.toDaqc()
}

/**
 * Android stationary sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidStationarySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {
    override fun convertData(data: FloatArray): BinaryState = BinaryState.On
}

/**
 * Android motion sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidMotionSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor) {
    override fun convertData(data: FloatArray): BinaryState = BinaryState.On
}

/**
 * Android heartbeat sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidHeartBeatSensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<DaqcQuantity<Dimensionless>>(manager, sensor) {
    override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()
}

/**
 * Android on-body sensor class
 *
 * @param manager The sensor manager for the android device.
 * @param sensor The sensor that you wish to monitor.
 */
class AndroidOnBodySensor(manager: SensorManager, sensor: Sensor) :
    AndroidSensor<BinaryState>(manager, sensor), BinaryStateInput {
    override fun convertData(data: FloatArray): BinaryState = if (data[0] == 1.0f) BinaryState.On else BinaryState.Off
}