package org.tenkiv.daqc.learning.controller

import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.RangedInput
import org.tenkiv.daqc.RangedOutput

class LearningController<T>(
    val targetInput: RangedInput<T>,
    val correlatedInputs: Collection<RangedInput<*>>,
    val outputs: Collection<RangedOutput<*>>
) : RangedOutput<T> where T : DaqcValue, T : Comparable<T> {


}