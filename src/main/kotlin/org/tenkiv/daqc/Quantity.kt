package org.tenkiv.daqc


import tec.uom.se.ComparableQuantity
import javax.measure.Quantity


fun <Q : Quantity<Q>> ComparableQuantity<Q>.toDaqcQuantity() = DaqcQuantity(this)
