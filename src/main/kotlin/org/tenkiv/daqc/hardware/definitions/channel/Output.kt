package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.DaqcValue

interface Output<in T : DaqcValue> {

    val isActive: Boolean

    fun setOutput(setting: T)

    fun deactivate()

}