package org.tenkiv.daqc.learning.controller

import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.Output
import org.tenkiv.daqc.RangedInput
import org.tenkiv.daqc.RangedOutput
import org.tenkiv.daqc.lib.toDuration
import org.tenkiv.daqc.lib.toPeriod
import java.time.Duration

class LearningController<T>(
    val targetInput: RangedInput<T>,
    val correlatedInputs: Collection<RangedInput<*>>,
    val outputs: Collection<RangedOutput<*>>,
    val minTimeBetweenActions: Duration = targetInput.sampleRate.toPeriod().toDuration()
) : Output<T> where T : DaqcValue, T : Comparable<T> {


}