package com.tenkiv.daqc.recording

import java.time.Duration
import java.time.Instant

data class Record<T : Any>(val pastDuration: Duration,
                            val storageFrequency: StorageFrequency = StorageFrequency.All) {

    private val values: MutableMap<Instant, T> = HashMap()

    fun getVaues(): Map<Instant, T> = values

    fun getValuesFrom(pastDuration: Duration): DurationValues<T> =
            if (pastDuration > this.pastDuration)
                DurationValues.FitsNot(values, pastDuration - this.pastDuration)
            else
                DurationValues.Fits(values.filter { it.key.isAfter(Instant.now() - pastDuration) })
}

sealed class DurationValues<out T>(private val map: Map<Instant, T>) : Map<Instant, T> by map {

    class Fits<out T>(values: Map<Instant, T>) : DurationValues<T>(values)

    class FitsNot<out T>(allValues: Map<Instant, T>, val missingHistDur: Duration) : DurationValues<T>(allValues)
}

sealed class StorageFrequency {

    object All: StorageFrequency()

    data class Interval(val interval: Duration): StorageFrequency()

    data class PerNumMeasurements(val number: Int): StorageFrequency()
}

sealed class StorageDuration {

    object Forever: StorageDuration()

    object Never: StorageDuration()

    data class For(val duration: Duration): StorageDuration()

}

