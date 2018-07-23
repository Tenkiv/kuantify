package org.tenkiv.daqc


import tec.units.indriya.ComparableQuantity
import javax.measure.Quantity


fun <Q : Quantity<Q>> ComparableQuantity<Q>.toDaqc() = DaqcQuantity(this)
