package com.tenkiv.daqc.networking

import com.tenkiv.FoundDevice
import com.tenkiv.LocatorUpdate
import com.tenkiv.LostDevice
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.daqcThreadContext
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.reflect.KClass

object Locator: Updatable<LocatorUpdate> {

    override val broadcastChannel: ConflatedBroadcastChannel<LocatorUpdate> = ConflatedBroadcastChannel()

    private val locatorList = ArrayList<DeviceLocator<Device>>()

    private val currentDevices = HashMap<KClass<*>, HashMap<String, Device>>()

    fun <T : DeviceLocator<Device>> addDeviceLocator(locator: T) {
        if (!locatorList.any { it::class == locator::class }) {
            locatorList.add(locator)
            launch(daqcThreadContext) { locator.broadcastChannel.consumeEach { rebroadcast(it) } }
        }
    }

    fun <T : DeviceLocator<Device>> removeDeviceLocator(locator: T) {
        locatorList.removeIf { it == locator }
    }

    private fun rebroadcast(device: LocatorUpdate) {
        when(device){
            is FoundDevice<*> -> currentDevices.
                    putIfAbsent(device.device::class,HashMap())?.
                    putIfAbsent(device.device.serialNumber, device.device)
            is LostDevice<*> -> currentDevices.
                    putIfAbsent(device.device::class,HashMap())?.
                    remove(device.device.serialNumber)
        }
    }



}