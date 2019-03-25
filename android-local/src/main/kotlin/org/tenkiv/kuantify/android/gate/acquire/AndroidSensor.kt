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

package org.tenkiv.kuantify.android.gate.acquire

import android.hardware.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
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
public abstract class LocalAndroidSensor<T : DaqcValue>(
    public final override val device: LocalAndroidDevice,
    public val sensor: Sensor,
    public final override val uid: String
) : AndroidInput<T>, LocalInput<T, LocalAndroidDevice> {

    private val manager: SensorManager get() = device.sensorManager

    /**
     * The sensor constant for the type of Android sensor.
     */
    public abstract val type: Int

    public final override val basePath: Path = listOf(RC.DAQC_GATE, uid)

    public final override val coroutineContext: CoroutineContext get() = device.coroutineContext

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    public final override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

    public final override val updateRate: UpdateRate by runningAverage()

    protected val _isTransceiving = Updatable(false)
    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val sensorListener = object : SensorEventListener {

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }

        override fun onSensorChanged(event: SensorEvent) {
            _updateBroadcaster.offer(convertData(event.values) at Instant.ofEpochSecond(event.timestamp))
        }

    }

    init {
        //This is done as there is only one Android sensor class making compile time safety impossible.
        @Suppress("LeakingThis")
        if (sensor.type != type) {
            throw AndroidSensorInitializationException("Sensor supplied was of the incorrect type.")
        }
    }

    public override fun startSampling() {
        _isTransceiving.value = true
        manager.registerListener(sensorListener, sensor, 100, 200)
    }

    public override fun stopTransceiving() {
        _isTransceiving.value = false
        manager.unregisterListener(sensorListener)
    }

    /**
     * Function to convert a given Android [SensorEvent] tuple into a usable [DaqcValue].
     *
     * @param data The [FloatArray] tuple given by a [SensorEvent].
     * @return The data in the form of a [DaqcValue].
     */
    public abstract fun convertData(data: FloatArray): T
}
