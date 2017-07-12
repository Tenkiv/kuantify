package com.tenkiv.daqc.networking

import com.tenkiv.DeviceFound
import com.tenkiv.LocatorUpdate
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.daqc.lib.openNewCoroutineListener
import com.tenkiv.daqcThreadContext
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.CopyOnWriteArrayList

abstract class DeviceLocator<T : Device> : Updatable<LocatorUpdate> {

    val activeDevices = CopyOnWriteArrayList<T>()

    abstract fun search()

    abstract fun stop()

    protected val awaitedDeviceMap = HashMap<String, ConflatedBroadcastChannel<Device>>()

    override val broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate>()

    fun awaitSpecificDevice(serialNumber: String): ConflatedBroadcastChannel<Device> {

        val channel = ConflatedBroadcastChannel<Device>()

        val potentialDevice = activeDevices.filter { it.serialNumber == serialNumber }.firstOrNull()

        if (potentialDevice != null) {
            channel.offer(potentialDevice); return channel
        }else{
            awaitedDeviceMap.put(serialNumber,channel)

            broadcastChannel.openNewCoroutineListener(daqcThreadContext,
                    {if(it is DeviceFound<Device>){awaitedDeviceMap[it.device.serialNumber]?.send(it.device)
                            awaitedDeviceMap[it.device.serialNumber]?.close()
                            awaitedDeviceMap.remove(it.device.serialNumber)}})

            return channel
        }
    }
}