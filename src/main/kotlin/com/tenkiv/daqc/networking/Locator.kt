package com.tenkiv.daqc.networking

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.daqcThreadContext
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.reflect.KClass

object Locator : Updatable<Device> {

    override val broadcastChannel: ConflatedBroadcastChannel<Device> = ConflatedBroadcastChannel()

    private val locatorList = ArrayList<DeviceLocator<List<Device>>>()


    private val currentDevices = HashMap<KClass<*>, HashMap<String, Device>>()

    fun <T : DeviceLocator<List<Device>>> addDeviceLocator(locator: T) {
        if (!locatorList.any { it::class == locator::class }) {
            locatorList.add(locator)
            launch(daqcThreadContext) { locator.broadcastChannel.consumeEach { broadcastNewDevices(it) } }
        }
    }

    fun <T : DeviceLocator<List<Device>>> removeDeviceLocator(locator: T) {
        locatorList.removeIf { it == locator }
    }

    fun broadcastNewDevices(devices: List<Device>) {

        devices.filter { !(currentDevices[devices.first()::class]?.containsValue(it) ?: return@filter false) }
                .forEach {
                    currentDevices.putIfAbsent(it::class, HashMap())?.putIfAbsent(it.serialNumber, it)
                    broadcastChannel.offer(it)
                }

    }



}