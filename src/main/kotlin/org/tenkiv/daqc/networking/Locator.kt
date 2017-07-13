package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import org.tenkiv.FoundDevice
import org.tenkiv.LocatorUpdate
import org.tenkiv.LostDevice
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device
import kotlin.reflect.KClass

object Locator : Updatable<LocatorUpdate> {

    override val broadcastChannel: ConflatedBroadcastChannel<LocatorUpdate> = ConflatedBroadcastChannel()

    private val locatorList = ArrayList<DeviceLocator<Device>>()

    private val currentDevices = HashMap<KClass<*>, HashMap<String, Device>>()

    //TODO: Throw exception if a locator of this type is already added.
    fun <T : DeviceLocator<Device>> addDeviceLocator(locator: T) {
        if (!locatorList.any { it::class == locator::class }) {
            locatorList.add(locator)
            launch(CommonPool) { locator.broadcastChannel.consumeEach { rebroadcast(it) } }
        }
    }

    fun <T : DeviceLocator<Device>> removeDeviceLocator(locator: T) {
        locatorList.removeIf { it == locator }
    }

    private fun rebroadcast(device: LocatorUpdate) {
        when (device) {
            is FoundDevice<*> -> currentDevices.
                    putIfAbsent(device.device::class, HashMap())?.
                    putIfAbsent(device.device.serialNumber, device.device)
            is LostDevice<*> -> currentDevices.
                    putIfAbsent(device.device::class, HashMap())?.
                    remove(device.device.serialNumber)
        }
    }


}