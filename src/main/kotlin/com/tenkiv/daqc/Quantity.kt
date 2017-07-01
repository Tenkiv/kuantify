package com.tenkiv.daqc


import org.tenkiv.coral.ValueInstant
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity

typealias QuantityMeasurement<Q> = ValueInstant<DaqcQuantity<Q>>

fun <Q : Quantity<Q>> ComparableQuantity<Q>.asDaqcQuantity() = DaqcQuantity(this)


class ValueOutOfRangeException(message: String? = null,
                               cause: Throwable? = null) : Throwable(message, cause)