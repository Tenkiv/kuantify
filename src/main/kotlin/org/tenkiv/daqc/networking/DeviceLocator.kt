package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import org.tenkiv.FoundDevice
import org.tenkiv.LocatorUpdate
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device
import kotlin.run

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
                }.firstOrNull() ?: run {
                    //TODO: Consider splitting this off into separate function.
                    val awaitingJob = broadcastChannel.open()
                    val iterator = awaitingJob.iterator()

                    while (iterator.hasNext() && this@async.isActive) {
                        val next = iterator.next()
                        if (next is FoundDevice<*> && next.serialNumber == serialNumber) {
                            awaitingJob.use {
                                return@run next
                            }
                        }
                    }
                    throw ClosedReceiveChannelException("Broadcast channel for DeviceLocator updates was closed " +
                            "while awaiting a specific device.")
                }

                device as? T ?: throw ClassCastException(
                        "Implementation error in class extending DeviceLocator.\n" +
                                " Type parameter T passed to _awaitSpecificTekdaqc is not the type returned by the" +
                                " implementation of DeviceLocator."
                )
            }
}