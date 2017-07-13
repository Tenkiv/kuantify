package com.tenkiv.daqc.recording

import com.tenkiv.QuantityMeasurement
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import javax.measure.Quantity


/*
inline fun <reified Q : Quantity<Q>> getDaqcValueRecorder(samplesInMemory: Int,
                                                          fileName: String,
                                                          updatable: Updatable<QuantityMeasurement<Q>>):
        MemoryRecorder<DaqcQuantity<Q>> {

    val fn: (String) -> DaqcQuantity<Q> = { serialized -> DaqcValue.quantityFromString<Q>(serialized) }
    return MemoryRecorder(samplesInMemory, fileName, fn, updatable)
}*/
