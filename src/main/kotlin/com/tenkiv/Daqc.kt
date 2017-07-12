package com.tenkiv

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.device.Device
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.coral.ValueInstant

typealias Measurement = ValueInstant<DaqcValue>
typealias BinaryMeasurement = ValueInstant<BinaryState>
typealias QuantityMeasurement<Q> = ValueInstant<DaqcQuantity<Q>>

internal val daqcThreadContext = newSingleThreadContext("Main Daqc Context")

class DaqcException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)


sealed class LocatorUpdate

data class DeviceFound<out T: Device>(val device: T): LocatorUpdate()

data class DeviceLost<out T: Device>(val device: T): LocatorUpdate()