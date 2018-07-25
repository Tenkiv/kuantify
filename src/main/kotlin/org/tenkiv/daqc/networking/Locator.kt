package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import org.tenkiv.daqc.FoundDevice
import org.tenkiv.daqc.LocatorUpdate
import org.tenkiv.daqc.LostDevice
import org.tenkiv.daqc.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device
import java.io.IOException
import kotlin.reflect.KClass

object Locator : Updatable<LocatorUpdate<*>> {

    private val _broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate<*>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out LocatorUpdate<*>>
        get() = _broadcastChannel

    private val locatorList = ArrayList<DeviceLocator>()

    private val currentDevices = HashMap<KClass<*>, HashMap<String, Device>>()

    // TODO: This needs to be made thread safe as it could be called from multiple threads.
    fun addDeviceLocator(locator: DeviceLocator) {
        if (!locatorList.any { it::class === locator::class }) {
            locatorList.add(locator)
            launch(CommonPool) { locator.broadcastChannel.consumeEach { rebroadcast(it) } }
        } else {
            throw IOException("Locator of same class already added.")
        }
    }

    // TODO: This needs to be made thread safe as it could be called from multiple threads.
    fun removeDeviceLocator(locator: DeviceLocator) {
        locatorList.removeIf { it == locator }
    }

    private fun rebroadcast(device: LocatorUpdate<*>) {
        when (device) {
            is FoundDevice -> currentDevices.putIfAbsent(device.wrappedDevice::class, HashMap())
                ?.putIfAbsent(device.serialNumber, device.wrappedDevice)
            is LostDevice -> currentDevices.putIfAbsent(device.wrappedDevice::class, HashMap())
                ?.remove(device.serialNumber)
        }
        _broadcastChannel.offer(device)
    }
}