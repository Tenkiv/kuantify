package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable

interface Output<T : DaqcValue> : Updatable<T> {

    suspend fun set(setting: T) = broadcastChannel.send(setting)

}