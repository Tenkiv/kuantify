package com.tenkiv.daqc.networking

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.LocatorParameters
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlin.reflect.KClass

class Locator internal constructor() : Updatable<Device> {

    override val broadcastChannel: ConflatedBroadcastChannel<Device> = ConflatedBroadcastChannel()

    private val locatorList = ArrayList<RemoteLocator<List<Device>>>()

    private val awaitedDeviceMap = HashMap<KClass<*>,HashMap<String,ConflatedBroadcastChannel<Device>>>()

    private val currentDevices = HashMap<KClass<*>,HashMap<String,Device>>()

    fun <T: RemoteLocator<List<Device>>> addDeviceLocator(locator: T){
        if(!locatorList.any { it::class == locator::class }){
            locatorList.add(locator)
            launch(DAQC_CONTEXT){ locator.broadcastChannel.consumeEach { broadcastNewDevices(it) } }
        }
    }

    fun <T: RemoteLocator<List<Device>>>removeDeviceLocator(locator: T){
        locatorList.removeIf { it == locator }
    }

    fun broadcastNewDevices(devices: List<Device>){

        devices.filter { !(currentDevices[devices.first()::class]?.containsValue(it) ?: return@filter false) }
                .forEach { currentDevices.putIfAbsent(it::class,HashMap())?.putIfAbsent(it.serialNumber,it)
                    broadcastChannel.offer(it) }

        devices.filter { awaitedDeviceMap[it::class]?.contains(it.serialNumber) ?: return@filter false }
                .forEach {
                    launch(DAQC_CONTEXT){
                        awaitedDeviceMap[it::class]?.get(it.serialNumber)?.send(it)
                        awaitedDeviceMap[it::class]?.get(it.serialNumber)?.close()
                        awaitedDeviceMap[it::class]?.remove(it.serialNumber)
                    }
                }
    }

    fun <T: Device>awaitSpecificDevice(deviceType: KClass<T>, serialNumber: String): ConflatedBroadcastChannel<Device>{

        val channel = ConflatedBroadcastChannel<Device>()

        val potentialDevice = currentDevices[deviceType]?.get(serialNumber)

        if(potentialDevice != null){ channel.offer(potentialDevice) ; return channel }

        awaitedDeviceMap.putIfAbsent(deviceType,HashMap())?.putIfAbsent(serialNumber,channel)

        return channel
    }

    fun createLocatorParameters(boardSpecificContext:Boolean): LocatorParameters{
        return LocatorParameters(boardSpecificContext)
    }
}