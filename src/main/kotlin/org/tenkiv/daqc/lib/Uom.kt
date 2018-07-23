package org.tenkiv.daqc.lib

import org.tenkiv.physikal.core.asType
import tec.units.indriya.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Frequency
import javax.measure.quantity.Time

fun Quantity<Frequency>.toPeriod(): Quantity<Time> = this.inverse().asType()

fun ComparableQuantity<Frequency>.toPeriod(): ComparableQuantity<Time> = this.inverse(Time::class.java)