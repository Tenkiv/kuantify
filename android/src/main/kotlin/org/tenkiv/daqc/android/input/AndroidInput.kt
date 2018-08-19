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
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.*
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.*
import tec.units.indriya.ComparableQuantity
import java.time.Instant
import javax.measure.quantity.Frequency

/**
 * Class which defines the basic aspects of any Android Sensor.
 */
abstract class AndroidSensor<Q : DaqcValue>(val manager: SensorManager, val sensor: Sensor) : Input<Q> {

    override val sampleRate: ComparableQuantity<Frequency> by runningAverage()

    private val _broadcastChannel = ConflatedBroadcastChannel<ValueInstant<Q>>()
    final override val broadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Q>>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    private val _accuracyBroadcastChannel = ConflatedBroadcastChannel<AndroidSensorAccuracy>()
    val accuracyBroadcastChannel: ConflatedBroadcastChannel<out AndroidSensorAccuracy>
        get() = _accuracyBroadcastChannel

    @Volatile
    var _isActive = false

    @Volatile
    var minimumAccuracy: AndroidSensorAccuracy =
        AndroidSensorAccuracy.LOW_ACCURACY

    override val isActive: Boolean
        get() = _isActive

    private val sensorListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            _accuracyBroadcastChannel.offer(AndroidSensorAccuracy.getAccuracy(accuracy))
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.accuracy >= minimumAccuracy.flag)
                _broadcastChannel.offer(convertData(event.values).at(Instant.ofEpochSecond(event.timestamp)))
        }
    }

    override fun activate() {
        _isActive = true
        manager.registerListener(sensorListener, sensor, 100, 200)
    }

    override fun deactivate() {
        _isActive = false
        manager.unregisterListener(sensorListener)
    }

    /**
     * Function to convert a given Android [SensorEvent] tuple into a usable [DaqcValue].
     *
     * @param data The [FloatArray] tuple given by a [SensorEvent].
     * @return The data in the form of a [DaqcValue].
     */
    abstract fun convertData(data: FloatArray): Q
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