package org.tenkiv

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device
import org.tenkiv.daqc.recording.Recorder


typealias Measurement = ValueInstant<DaqcValue>
typealias BinaryMeasurement = ValueInstant<BinaryState>
typealias QuantityMeasurement<Q> = ValueInstant<DaqcQuantity<Q>>

internal val daqcThreadContext = newSingleThreadContext("Main Daqc Context")

//TODO: Transmit critical errors over this.
val daqcCriticalErrorBroadcastChannel = ConflatedBroadcastChannel<DaqcCriticalError>()

private var memRecorderUid = 0L

internal fun getMemoryRecorderUid(): String {
    return (memRecorderUid++).toString()
}

sealed class LocatorUpdate

data class FoundDevice<out T : Device>(val device: T) : LocatorUpdate()

data class LostDevice<out T : Device>(val device: T) : LocatorUpdate()


class DaqcException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

data class RecordedUpdatable<T, out U : Updatable<ValueInstant<T>>>(val updatable: U,
                                                                    val recorder: Recorder<ValueInstant<T>>)

enum class DaqcCriticalError {
    FAILED_TO_REINITIALIZE,
    FAILED_MAJOR_COMMAND,
    TERMINAL_CONNECTION_DISRUPTION,
    PARTIAL_DISCONNECTION
}
