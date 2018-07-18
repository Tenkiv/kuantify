package org.tenkiv.daqc.lib

import org.tenkiv.physikal.core.*
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.MetricPrefix.NANO
import tec.uom.se.unit.Units.SECOND
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.measure.Quantity
import javax.measure.quantity.Frequency
import javax.measure.quantity.Time

fun Quantity<Frequency>.toPeriod(): Quantity<Time> = this.inverse().asType()

fun ComparableQuantity<Frequency>.toPeriod(): ComparableQuantity<Time> = this.inverse(Time::class.java)

//TODO: Use this function from Physikal
fun Quantity<Time>.toDuration(): Duration {

    val secondsLong = this toLongIn SECOND
    val nanosLong = (this - secondsLong.second) toLongIn NANO(SECOND)

    return Duration.ofSeconds(secondsLong, nanosLong)
}

fun Duration.toQuantity(): ComparableQuantity<Time> =
    this[ChronoUnit.SECONDS].second + this[ChronoUnit.NANOS].nano.second