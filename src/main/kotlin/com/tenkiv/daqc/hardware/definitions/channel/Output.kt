package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue

interface Output<in T : DaqcValue> {

    fun setOutput(setting: T)

}