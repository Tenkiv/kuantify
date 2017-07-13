package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.LocatorUpdate
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device
import java.util.concurrent.CopyOnWriteArrayList

abstract class DeviceLocator<T : Device> : Updatable<LocatorUpdate> {

    val activeDevices = CopyOnWriteArrayList<T>()

    abstract fun search()

    abstract fun stop()

    protected val awaitedDeviceMap = HashMap<String, ConflatedBroadcastChannel<Device>>()

    override val broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate>()

    suspend fun awaitSpecificDevice(serialNumber: String): Device {

        /*val potentialDevice = activeDevices.filter { it.serialNumber == serialNumber }.firstOrNull()

        if (potentialDevice != null) {
            channel.offer(potentialDevice); return channel
        } else {
            awaitedDeviceMap.put(serialNumber, channel)

            broadcastChannel.openNewCoroutineListener(daqcThreadContext) {
                if (it is FoundDevice<Device>) {
                    awaitedDeviceMap[it.device.serialNumber]?.send(it.device)
                    awaitedDeviceMap[it.device.serialNumber]?.close()
                    awaitedDeviceMap.remove(it.device.serialNumber)
                }
            }

            return channel*/
        TODO("Make this work.")
    }

}