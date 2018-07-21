package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.hardware.definitions.device.Device


typealias Measurement = ValueInstant<DaqcValue>
typealias BinaryStateMeasurement = ValueInstant<BinaryState>
typealias QuantityMeasurement<Q> = ValueInstant<DaqcQuantity<Q>>

internal val daqcThreadContext = newSingleThreadContext("Main Daqc Context")

val daqcCriticalErrorBroadcastChannel = ConflatedBroadcastChannel<DaqcCriticalError>()

internal object Daqc {

    /**
     * @return null if the number to be normalised is outside the range.
     */
    fun normaliseOrNull(number: Double, range: ClosedRange<Double>): Double? {

        return if (number in range) {
            normalise(number, range)
        } else {
            null
        }
    }

    fun normalise(number: Double, range: ClosedRange<Double>): Double {
        val min = range.start
        val max = range.endInclusive

        return normalise(number, min, max)
    }

    fun normalise(number: Double, min: Double, max: Double): Double = (number - min) / (max - min)

}

interface RangedIO<T> : Updatable<ValueInstant<T>> where T : DaqcValue, T : Comparable<T> {

    /**
     * The range of values that this IO is likely to handle. There is not necessarily a guarantee that there will
     * never be a value outside this range.
     */
    val valueRange: ClosedRange<T>

    /**
     * @return a [Double] representation of the current value normalised to be between 0 and 1 based on the
     * [valueRange], null if the updatable does not yet have a value or the value is outside the [valueRange].
     */
    fun getNormalisedDoubleOrNull(): Double? {
        val value = valueOrNull?.value

        if (value is BinaryState?)
            return value?.toDouble()

        val min = valueRange.start.toDoubleInSystemUnit()
        val max = valueRange.endInclusive.toDoubleInSystemUnit()
        val valueDouble = value?.toDoubleInSystemUnit()

        return if (valueDouble != null && valueDouble >= min && valueDouble <= max) {
            Daqc.normalise(valueDouble, min, max)
        } else {
            null
        }
    }

}

sealed class LocatorUpdate<out T : Device>(val wrappedDevice: T) : Device by wrappedDevice

/**
 * A Device found by a Locator
 */
class FoundDevice<out T : Device>(device: T) : LocatorUpdate<T>(device) {

    override fun toString() = "FoundDevice: $wrappedDevice"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FoundDevice<*>

        if (wrappedDevice != other.wrappedDevice) return false

        return true
    }

    override fun hashCode(): Int {
        return wrappedDevice.hashCode()
    }
}

class LostDevice<out T : Device>(device: T) : LocatorUpdate<T>(device) {

    override fun toString() = "LostDevice: $wrappedDevice"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LostDevice<*>

        if (wrappedDevice != other.wrappedDevice) return false

        return true
    }

    override fun hashCode(): Int {
        return wrappedDevice.hashCode()
    }


}


open class DaqcException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)


sealed class DaqcCriticalError : DaqcException() {

    abstract val device: Device

    data class FailedToReinitialize(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    data class FailedMajorCommand(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    data class TerminalConnectionDisruption(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

    data class PartialDisconnection(
        override val device: Device,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : DaqcCriticalError()

}
