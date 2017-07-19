package org.tenkiv

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.device.Device


typealias Measurement = ValueInstant<DaqcValue>
typealias BinaryStateMeasurement = ValueInstant<BinaryState>
typealias QuantityMeasurement<Q> = ValueInstant<DaqcQuantity<Q>>

internal val daqcThreadContext = newSingleThreadContext("Main Daqc Context")

//TODO: Transmit critical errors over this.
val daqcCriticalErrorBroadcastChannel = ConflatedBroadcastChannel<DaqcCriticalError>()

private var memRecorderUid = 0L

internal fun getMemoryRecorderUid(): String {
    return (memRecorderUid++).toString()
}

sealed class LocatorUpdate<out T : Device>(val capturedDevice: T) : Device by capturedDevice

class FoundDevice<out T : Device>(device: T) : LocatorUpdate<T>(device)

class LostDevice<out T : Device>(device: T) : LocatorUpdate<T>(device)


class DaqcException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

enum class DaqcCriticalError {
    FAILED_TO_REINITIALIZE,
    FAILED_MAJOR_COMMAND,
    TERMINAL_CONNECTION_DISRUPTION,
    PARTIAL_DISCONNECTION
}
