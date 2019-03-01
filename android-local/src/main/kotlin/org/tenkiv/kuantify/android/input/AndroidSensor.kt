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

package org.tenkiv.kuantify.android.input

import android.hardware.*
import android.hardware.SensorManager.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.android.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.acquire.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import java.time.*
import kotlin.coroutines.*

/**
 * Class which defines the basic aspects of any Android Sensor.
 */
@Suppress("LeakingThis")
abstract class LocalAndroidSensor<T : DaqcValue>(
    val device: LocalAndroidDevice,
    val sensor: Sensor,
    override val uid: String
) : AndroidSensor<T>, LocalInput<T> {

    private val manager: SensorManager get() = device.sensorManager

    /**
     * The sensor constant for the type of Android sensor.
     */
    abstract val type: Int

    override val basePath: Path = listOf(RC.DAQC_GATE, uid)

    override val coroutineContext: CoroutineContext get() = device.coroutineContext

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    final override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

    override val updateRate: UpdateRate by runningAverage()

    private val _accuracyBroadcastChannel = ConflatedBroadcastChannel<AndroidSensorAccuracy>()

    /**
     * Channel which broadcasts the current accuracy of the sensor.
     */
    val accuracyBroadcastChannel: ConflatedBroadcastChannel<out AndroidSensorAccuracy>
        get() = _accuracyBroadcastChannel

    protected val _isTransceiving = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            _accuracyBroadcastChannel.offer(AndroidSensorAccuracy.getAccuracy(accuracy))
        }

        override fun onSensorChanged(event: SensorEvent) {
            _updateBroadcaster.offer(convertData(event.values).at(Instant.ofEpochSecond(event.timestamp)))
        }
    }

    init {
        //This is done as there is only one Android sensor class making compile time safety impossible.
        if (sensor.type != type)
            throw AndroidSensorInitializationException("Sensor supplied was of the incorrect type.")
    }

    override fun startSampling() {
        _isTransceiving.value = true
        manager.registerListener(sensorListener, sensor, 100, 200)
    }

    override fun stopTransceiving() {
        _isTransceiving.value = false
        manager.unregisterListener(sensorListener)
    }

    /**
     * Function to convert a given Android [SensorEvent] tuple into a usable [DaqcValue].
     *
     * @param data The [FloatArray] tuple given by a [SensorEvent].
     * @return The data in the form of a [DaqcValue].
     */
    abstract fun convertData(data: FloatArray): T
}

/**
 * Enum class wrapper for Android sensor accuracy constants.
 */
enum class AndroidSensorAccuracy(val flag: Int) {
    /**
     * Highly accurate samples
     */
    HIGH_ACCURACY(SENSOR_STATUS_ACCURACY_HIGH),
    /**
     * Moderately accurate samples
     */
    MEDIUM_ACCURACY(SENSOR_STATUS_ACCURACY_MEDIUM),
    /**
     * Low accuracy samples.
     */
    LOW_ACCURACY(SENSOR_STATUS_ACCURACY_LOW),
    /**
     * Extremely unreliable readings.
     */
    UNRELIABLE(SENSOR_STATUS_UNRELIABLE),
    /**
     * No contact with the sensor.
     */
    NO_CONTACT(SENSOR_STATUS_NO_CONTACT),
    /**
     * An invalid value.
     */
    INVALID(Int.MIN_VALUE);

    companion object {
        /**
         * Function which returns the [AndroidSensorAccuracy] given an Android constant value.
         *
         * @param flag The Android constant flag.
         * @return An [AndroidSensorAccuracy] reading. Returns [AndroidSensorAccuracy.INVALID] if value was not found.
         */
        fun getAccuracy(flag: Int): AndroidSensorAccuracy = values().firstOrNull { it.flag == flag } ?: INVALID
    }
}