package org.tenkiv.kuantify.android.lib

import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import tec.units.indriya.format.*
import tec.units.indriya.unit.*
import tec.units.indriya.unit.Units.*
import javax.measure.quantity.*

val BEATS_PER_MINUTE: AlternateUnit<Frequency> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val unit = AlternateUnit<Frequency>(HERTZ * 60.0, "bpm")
    SimpleUnitFormat.getInstance().label(unit, "bpm")
    unit
}

val Number.beatsPerMinute: ComparableQuantity<Frequency>
    get() = this(BEATS_PER_MINUTE)