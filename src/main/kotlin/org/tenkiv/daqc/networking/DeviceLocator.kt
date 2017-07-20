package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import org.tenkiv.FoundDevice
import org.tenkiv.LocatorUpdate
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device

abstract class DeviceLocator : Updatable<LocatorUpdate<*>> {

    /**
     * Must be thread safe, this will be accessed from multiple threads.
     */
    abstract val activeDevices: List<Device>

    abstract fun search()

    abstract fun stop()

    abstract fun awaitSpecificDevice(serialNumber: String): Deferred<Device>

    protected inline fun <reified T : Device> _awaitSpecificDevice(serialNumber: String): Deferred<T> =
            async(CommonPool) {
                val device = activeDevices.filter {
                    it.serialNumber == serialNumber
                }.firstOrNull() ?: awaitBroadcast(this@async, serialNumber)

                device as? T ?: throw ClassCastException(
                        "Implementation error in class extending DeviceLocator.\n" +
                                " Type parameter T passed to _awaitSpecificTekdaqc is not the type returned by the" +
                                " implementation of DeviceLocator."
                )
            }

    //TODO: Consider making this an inner function of _awaitSpecificDevice.
    protected suspend fun awaitBroadcast(job: CoroutineScope, serialNumber: String): Device {
        val awaitingJob = broadcastChannel.open()
        val iterator = awaitingJob.iterator()

        while (iterator.hasNext() && job.isActive) {
            val next = iterator.next()
            if (next is FoundDevice<*> && next.serialNumber == serialNumber) {
                awaitingJob.use {
                    return next
                }
            }
        }
        throw ClosedReceiveChannelException("Broadcast channel for DeviceLocator updates was closed " +
                "while awaiting a specific device.")
    }

}