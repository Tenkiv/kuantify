package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable

/**
 * Created by tenkiv on 4/17/17.
 */
class ThermocoupleK : Sensor<DaqcValue>(emptyList()) {
    suspend override fun onUpdate(updatable: Updatable<DaqcValue>, value: DaqcValue) {}
}