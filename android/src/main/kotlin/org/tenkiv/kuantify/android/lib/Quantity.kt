package org.tenkiv.kuantify.android.lib

import org.tenkiv.physikal.core.*
import tec.units.indriya.ComparableQuantity
import tec.units.indriya.unit.AlternateUnit
import tec.units.indriya.unit.Units.*
import javax.measure.quantity.Frequency

val BEATS_PER_MINUTE = AlternateUnit<Frequency>(HERTZ * 60.0, "bpm")

val Number.beatsPerMinute: ComparableQuantity<Frequency>
    get() = this(BEATS_PER_MINUTE)