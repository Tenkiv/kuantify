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

sealed class LocatorUpdate<out T : Device>(val wrappedDevice: T) : Device by wrappedDevice

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


    data class FailedToReinitialize(override val device: Device,
                                    override val message: String? = null,
                                    override val cause: Throwable? = null) : DaqcCriticalError()

    data class FailedMajorCommand(override val device: Device,
                                  override val message: String? = null,
                                  override val cause: Throwable? = null) : DaqcCriticalError()

    data class TerminalConnectionDisruption(override val device: Device,
                                            override val message: String? = null,
                                            override val cause: Throwable? = null) : DaqcCriticalError()

    data class PartialDisconnection(override val device: Device,
                                    override val message: String? = null,
                                    override val cause: Throwable? = null) : DaqcCriticalError()

}
