package com.tenkiv.daqc.networking

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.daqc.lib.openNewCoroutineListener
import com.tenkiv.daqcThreadContext
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch

abstract class DeviceLocator<T : List<Device>> : Updatable<T> {

    abstract val activeDevices: T

    abstract fun search()

    abstract fun stop()

    protected val awaitedDeviceMap = HashMap<String, ConflatedBroadcastChannel<Device>>()

    override val broadcastChannel = ConflatedBroadcastChannel(activeDevices)

    fun awaitSpecificDevice(serialNumber: String): ConflatedBroadcastChannel<Device> {

        val channel = ConflatedBroadcastChannel<Device>()

        val potentialDevice = activeDevices.filter { it.serialNumber == serialNumber }.firstOrNull()

        if (potentialDevice != null) {
            channel.offer(potentialDevice); return channel
        }else{
            awaitedDeviceMap.put(serialNumber,channel)

            broadcastChannel.openNewCoroutineListener(daqcThreadContext,
                    onReceive = {it.filter { awaitedDeviceMap.contains(it.serialNumber)}
                            .forEach {
                                launch(daqcThreadContext) {
                                    awaitedDeviceMap[it.serialNumber]?.send(it)
                                    awaitedDeviceMap[it.serialNumber]?.close()
                                    awaitedDeviceMap.remove(it.serialNumber)
                                }
                            }})

            return channel
        }
    }
}