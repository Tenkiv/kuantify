package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.FoundDevice
import org.tenkiv.LocatorUpdate
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device
import org.tenkiv.daqcThreadContext
import java.util.concurrent.CopyOnWriteArrayList

abstract class DeviceLocator : Updatable<LocatorUpdate> {

    val activeDevices = CopyOnWriteArrayList<Device>()

    abstract fun search()

    abstract fun stop()

    override val broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate>()

    suspend fun awaitSpecificDevice(serialNumber: String): Deferred<Device> {
        return async(daqcThreadContext) {
            return@async activeDevices.filter {
                it.serialNumber == serialNumber
            }.firstOrNull() ?: awaitBroadcast(this, serialNumber)
        }
    }

    private suspend fun awaitBroadcast(job: CoroutineScope, serialNumber: String): Device {
        val awaitingJob = broadcastChannel.open()
        val iterator = awaitingJob.iterator()

        while (iterator.hasNext() && job.isActive) {
            val next = iterator.next()
            if (next is FoundDevice && next.device.serialNumber == serialNumber) {
                awaitingJob.use {
                    return next.device
                }
            }
        }
        throw ClosedReceiveChannelException("Broadcast channel for DeviceLocator updates was closed " +
                "while awaiting a specific device.")
    }
}