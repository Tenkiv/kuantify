package org.tenkiv.kuantify.android.device

import android.annotation.*
import android.content.*
import android.hardware.*
import android.provider.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.android.*
import org.tenkiv.kuantify.android.input.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*

open class LocalAndroidDevice internal constructor(context: Context) : LocalDevice(),
    AndroidDevice {

    internal val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        ?: throw ClassCastException(
            "Android has somehow returned the wrong system service; this is not our problem."
        )

    override val ambientTemperatureSensors: List<LocalAndroidAmbientTemperatureSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_AMBIENT_TEMPERATURE).mapIndexed { index, value ->
            LocalAndroidAmbientTemperatureSensor(
                this,
                value,
                "${AndroidSensorTypeId.AMBIENT_TEMPERATURE}$index"
            )
        }

    override val heartRateSensors: List<LocalAndroidHeartRateSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_HEART_RATE).mapIndexed { index, value ->
            LocalAndroidHeartRateSensor(
                this,
                value,
                "${AndroidSensorTypeId.HEART_RATE}$index"
            )
        }

    override val lightSensors: List<LocalAndroidLightSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_LIGHT).mapIndexed { index, value ->
            LocalAndroidLightSensor(
                this,
                value,
                "${AndroidSensorTypeId.LIGHT}$index"
            )
        }

    override val proximitySensors: List<LocalAndroidProximitySensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PROXIMITY).mapIndexed { index, value ->
            LocalAndroidProximitySensor(
                this,
                value,
                "${AndroidSensorTypeId.PROXIMITY}$index"
            )
        }

    override val pressureSensors: List<LocalAndroidPressureSensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_PRESSURE).mapIndexed { index, value ->
            LocalAndroidPressureSensor(
                this,
                value,
                "${AndroidSensorTypeId.PRESSURE}$index"
            )
        }

    override val relativeHumiditySensors: List<LocalAndroidRelativeHumiditySensor> =
        sensorManager.getDynamicSensorList(Sensor.TYPE_RELATIVE_HUMIDITY).mapIndexed { index, value ->
            LocalAndroidRelativeHumiditySensor(
                this,
                value,
                "${AndroidSensorTypeId.RELATIVE_HUMIDITY}$index"
            )
        }

    override fun getInfo(): String {
        val info = AndroidDevice.Info(
            deviceId = uid,
            numAmbientTemperatureSensors = ambientTemperatureSensors.size,
            numHeartRateSensors = heartRateSensors.size,
            numLightSensors = lightSensors.size,
            numProximitySensors = proximitySensors.size,
            numPressureSensors = pressureSensors.size,
            numRelativeHumiditySensors = relativeHumiditySensors.size
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

    override fun sideConfig(config: SideRouteConfig) {
        super.sideConfig(config)
        ambientTemperatureSensors.applySideConfigsTo(config)
        heartRateSensors.applySideConfigsTo(config)
        lightSensors.applySideConfigsTo(config)
        pressureSensors.applySideConfigsTo(config)
        proximitySensors.applySideConfigsTo(config)
        relativeHumiditySensors.applySideConfigsTo(config)
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
    }

}

/**
 * Exceptions thrown by [LocalAndroidSensor]s when a fatal error occurs.
 */
sealed class AndroidSensorException(error: String) : Exception(error)

/**
 * Class which is thrown when a sensor is improperly initialized.
 */
class AndroidSensorInitializationException(error: String) : AndroidSensorException(error)