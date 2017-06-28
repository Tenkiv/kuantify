package com.tenkiv.daqc


import kotlinx.coroutines.experimental.channels.SendChannel
import org.tenkiv.coral.ValueInstant
import tec.uom.se.ComparableQuantity


typealias QuantityMeasurement<Q> = ValueInstant<ComparableQuantity<Q>>

typealias MeasurementSendChannel<E> = SendChannel<ValueInstant<E>>

typealias QuantMeasureSendChannel<Q> = SendChannel<ValueInstant<ComparableQuantity<Q>>>

typealias ClosedQuantityRange<Q> = ClosedRange<ComparableQuantity<Q>>



class ValueOutOfRangeException(message: String? = null,
                               cause: Throwable? = null) : Throwable(message, cause)