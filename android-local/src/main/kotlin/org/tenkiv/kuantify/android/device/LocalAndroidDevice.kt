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
 *
 */

package org.tenkiv.kuantify.android.device

import android.annotation.*
import android.content.*
import android.hardware.*
import android.hardware.camera2.*
import android.provider.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.android.gate.*
import org.tenkiv.kuantify.android.gate.acquire.*
import org.tenkiv.kuantify.android.gate.control.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.networking.configuration.*

private typealias SensorConstructor<S> = (LocalAndroidDevice, Sensor, String) -> S

open class LocalAndroidDevice internal constructor(context: Context) : LocalDevice(), AndroidDevice {

    internal val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        ?: wrongSystemService()

    internal val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        ?: wrongSystemService()

    final override val ambientTemperatureSensors: List<LocalAndroidAmbientTemperatureSensor> =
        getSensors(
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            AndroidGateTypeId.AMBIENT_TEMPERATURE
        ) { device, sensor, id ->
            LocalAndroidAmbientTemperatureSensor(device, sensor, id)
        }

    final override val heartRateSensors: List<LocalAndroidHeartRateSensor> =
        getSensors(
            Sensor.TYPE_HEART_RATE,
            AndroidGateTypeId.HEART_RATE
        ) { device, sensor, id ->
            LocalAndroidHeartRateSensor(device, sensor, id)
        }

    final override val lightSensors: List<LocalAndroidLightSensor> =
        getSensors(
            Sensor.TYPE_LIGHT,
            AndroidGateTypeId.LIGHT
        ) { device, sensor, id ->
            LocalAndroidLightSensor(device, sensor, id)
        }

    final override val proximitySensors: List<LocalAndroidProximitySensor> =
        getSensors(
            Sensor.TYPE_PROXIMITY,
            AndroidGateTypeId.PROXIMITY
        ) { device, sensor, id ->
            LocalAndroidProximitySensor(device, sensor, id)
        }

    final override val pressureSensors: List<LocalAndroidPressureSensor> =
        getSensors(
            Sensor.TYPE_PRESSURE,
            AndroidGateTypeId.PRESSURE
        ) { device, sensor, id ->
            LocalAndroidPressureSensor(device, sensor, id)
        }

    final override val relativeHumiditySensors: List<LocalAndroidRelativeHumiditySensor> =
        getSensors(
            Sensor.TYPE_RELATIVE_HUMIDITY,
            AndroidGateTypeId.RELATIVE_HUMIDITY
        ) { device, sensor, id ->
            LocalAndroidRelativeHumiditySensor(device, sensor, id)
        }

    final override val torchControllers: List<AndroidTorchController> = buildTorchControllers()

    private inline fun <T : DaqcValue, S : LocalAndroidSensor<T>> getSensors(
        sensorType: Int,
        sensorTypeId: String,
        sensorConstructor: SensorConstructor<S>
    ): List<S> {
        var number = 0
        val result = ArrayList<S>()

        sensorManager.getSensorList(sensorType).forEach { value ->
            result += sensorConstructor(
                this,
                value,
                "$sensorTypeId$number"
            )
            number++
        }

        sensorManager.getDynamicSensorList(sensorType).forEach { value ->
            result += sensorConstructor(
                this,
                value,
                "$sensorTypeId$number"
            )
            number++
        }

        return result
    }

    private fun buildTorchControllers(): List<AndroidTorchController> {
        val result = ArrayList<AndroidTorchController>()

        cameraManager.cameraIdList.forEachIndexed { i, cameraId ->
            val hasFlash = cameraManager.getCameraCharacteristics(cameraId)[CameraCharacteristics.FLASH_INFO_AVAILABLE]

            if (hasFlash == true) {
                result += AndroidTorchController(this, "${AndroidGateTypeId.TORCH}$i", cameraId)
            }
        }

        return result
    }

    override fun getInfo(): String {
        val info = AndroidDevice.Info(
            deviceId = uid,
            numAmbientTemperatureSensors = ambientTemperatureSensors.size,
            numHeartRateSensors = heartRateSensors.size,
            numLightSensors = lightSensors.size,
            numProximitySensors = proximitySensors.size,
            numPressureSensors = pressureSensors.size,
            numRelativeHumiditySensors = relativeHumiditySensors.size,
            numTorchControllers = torchControllers.size
        )

        return Json.stringify(AndroidDevice.Info.serializer(), info)
    }

    /**
     * We are using a hardware ID as opposed to an installation UID as we have to try to ensure a UUID against other
     * Android devices, not just across applications. We are obviously also not using this UUID for advertising so the
     * Ad UID supplied by the system would also be incorrect. Our use case fits most similarly with telephony cases and
     * as such we use the Android ID.
     * More Info: https://developer.android.com/training/articles/user-data-ids
     */
    @SuppressLint("HardwareIds")
    final override val uid: String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)
        ambientTemperatureSensors.addSideRoutingTo(routing)
        heartRateSensors.addSideRoutingTo(routing)
        lightSensors.addSideRoutingTo(routing)
        pressureSensors.addSideRoutingTo(routing)
        proximitySensors.addSideRoutingTo(routing)
        relativeHumiditySensors.addSideRoutingTo(routing)
        torchControllers.addRoutingTo(routing)
    }

    companion object {
        @Volatile
        private var device: LocalAndroidDevice? = null

        fun get(applicationContext: Context): LocalAndroidDevice {
            return device ?: run {
                val newDevice = LocalAndroidDevice(applicationContext)
                device = newDevice
                newDevice
            }
        }

        private fun wrongSystemService(): Nothing {
            throw ClassCastException("Android returned the wrong system service; this is an Android system problem.")
        }
    }

}

/**
 * Exceptions thrown by [LocalAndroidSensor]s when a fatal error occurs.
 */
open class AndroidSensorException(error: String) : Exception(error)

/**
 * Class which is thrown when a sensor is improperly initialized.
 */
class AndroidSensorInitializationException(error: String) : AndroidSensorException(error)