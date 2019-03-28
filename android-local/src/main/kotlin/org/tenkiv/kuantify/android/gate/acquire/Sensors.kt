/*
 * Copyright 2019 Tenkiv, Inc.
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

package org.tenkiv.kuantify.android.gate.acquire

import android.hardware.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.android.lib.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.acquire.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.physikal.core.*
import javax.measure.quantity.*

/**
 * Android ambient temperature sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
public class LocalAndroidAmbientTemperatureSensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Temperature>>(device, sensor, uid),
    LocalQuantityInput<Temperature, LocalAndroidDevice> {

    public override val type: Int get() = Sensor.TYPE_AMBIENT_TEMPERATURE

    public override fun convertData(data: FloatArray): DaqcQuantity<Temperature> = data[0].celsius.toDaqc()

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super<LocalQuantityInput>.sideRouting(routing)
    }
}

public class LocalAndroidHeartRateSensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Frequency>>(device, sensor, uid),
    LocalQuantityInput<Frequency, LocalAndroidDevice> {

    public override val type: Int get() = Sensor.TYPE_HEART_RATE

    public override fun convertData(data: FloatArray): DaqcQuantity<Frequency> = data[0].beatsPerMinute.toDaqc()

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super<LocalQuantityInput>.sideRouting(routing)
    }
}

/**
 * Android light sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
public class LocalAndroidLightSensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Illuminance>>(device, sensor, uid),
    LocalQuantityInput<Illuminance, LocalAndroidDevice> {

    public override val type: Int get() = Sensor.TYPE_LIGHT

    public override fun convertData(data: FloatArray): DaqcQuantity<Illuminance> = data[0].lux.toDaqc()

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super<LocalQuantityInput>.sideRouting(routing)
    }
}

/**
 * Android proximity sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
public class LocalAndroidProximitySensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Length>>(device, sensor, uid),
    LocalQuantityInput<Length, LocalAndroidDevice> {

    public override val type: Int get() = Sensor.TYPE_PROXIMITY

    public override fun convertData(data: FloatArray): DaqcQuantity<Length> = data[0].centi.metre.toDaqc()

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super<LocalQuantityInput>.sideRouting(routing)
    }
}

/**
 * Android pressure sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
public class LocalAndroidPressureSensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Pressure>>(device, sensor, uid),
    LocalQuantityInput<Pressure, LocalAndroidDevice> {

    public override val type: Int get() = Sensor.TYPE_PRESSURE

    public override fun convertData(data: FloatArray): DaqcQuantity<Pressure> = data[0].hecto.pascal.toDaqc()

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super<LocalQuantityInput>.sideRouting(routing)
    }
}

/**
 * Android relative humidity sensor.
 *
 * @param manager The Android device's [SensorManager].
 * @param sensor The sensor to be integrated.
 */
public class LocalAndroidRelativeHumiditySensor(device: LocalAndroidDevice, sensor: Sensor, uid: String) :
    LocalAndroidSensor<DaqcQuantity<Dimensionless>>(device, sensor, uid),
    LocalQuantityInput<Dimensionless, LocalAndroidDevice> {

    public override val type: Int get() = Sensor.TYPE_RELATIVE_HUMIDITY

    public override fun convertData(data: FloatArray): DaqcQuantity<Dimensionless> = data[0].percent.toDaqc()

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super<LocalQuantityInput>.sideRouting(routing)
    }
}