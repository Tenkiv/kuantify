package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue

interface Output<in T : DaqcValue> {

    val isActive: Boolean

    fun setOutput(setting: T)

    fun deactivate()

}