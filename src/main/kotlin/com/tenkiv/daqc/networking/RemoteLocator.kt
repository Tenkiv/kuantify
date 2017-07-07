package com.tenkiv.daqc.networking

import com.tenkiv.LocatorParameters
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel

abstract class RemoteLocator<T : List<Device>>(params: LocatorParameters) : Updatable<T> {

    abstract val activeDevices: T

    abstract fun search()

    abstract fun stop()

    override val broadcastChannel = ConflatedBroadcastChannel(activeDevices)

}