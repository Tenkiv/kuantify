package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import kotlinx.coroutines.experimental.channels.SendChannel

interface Output<in T : DaqcValue> {

    val commandChannel: SendChannel<T>

    suspend fun setOutput(setting: T) = commandChannel.send(setting)

}