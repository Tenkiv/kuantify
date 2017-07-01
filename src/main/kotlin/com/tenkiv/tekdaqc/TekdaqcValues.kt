package com.tenkiv.tekdaqc

import com.tenkiv.daqc.AnalogAccuracy
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Gain
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Rate
import com.tenkiv.tekdaqc.hardware.ATekdaqc.AnalogScale

/**
 * Created by tenkiv on 6/30/17.
 */
val analogAccuracyLookupTable =
        mapOf<AnalogAccuracy, Map<AnalogScale, Map<Gain,Rate>>>()