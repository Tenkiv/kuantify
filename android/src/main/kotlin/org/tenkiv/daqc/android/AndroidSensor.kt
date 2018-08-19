package org.tenkiv.daqc.android

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.*
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.Input
import org.tenkiv.daqc.runningAverage
import tec.units.indriya.ComparableQuantity
import java.time.Instant
import javax.measure.quantity.Frequency

abstract class AndroidSensor<Q : DaqcValue>(val manager: SensorManager, val sensor: Sensor) : Input<Q> {

    /**
     * The sensor constant for the type of Android sensor.
     */
    abstract val type: Int

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

    init {
        //This is done as there is only one Android sensor class making compile time safety impossible.
        if (sensor.type != type)
            throw AndroidSensorException("Sensor supplied was of the incorrect type.")
    }

    abstract fun convertData(data: FloatArray): Q
}

enum class AndroidSensorAccuracy(val flag: Int) {
    HIGH_ACCURACY(SENSOR_STATUS_ACCURACY_HIGH),
    MEDIUM_ACCURACY(SENSOR_STATUS_ACCURACY_MEDIUM),
    LOW_ACCURACY(SENSOR_STATUS_ACCURACY_LOW),
    UNRELIABLE(SENSOR_STATUS_UNRELIABLE),
    NO_CONTACT(SENSOR_STATUS_NO_CONTACT),
    INVALID(Int.MIN_VALUE);

    companion object {
        fun getAccuracy(flag: Int): AndroidSensorAccuracy = values().firstOrNull { it.flag == flag } ?: INVALID
    }
}