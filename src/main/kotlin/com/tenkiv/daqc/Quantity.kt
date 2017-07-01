package com.tenkiv.daqc


import tec.uom.se.ComparableQuantity
import javax.measure.Quantity

fun <Q : Quantity<Q>> ComparableQuantity<Q>.asDaqcQuantity() = DaqcQuantity(this)


class ValueOutOfRangeException(message: String? = null,
                               cause: Throwable? = null) : Throwable(message, cause)