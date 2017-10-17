package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import org.tenkiv.daqc.FoundDevice
import org.tenkiv.daqc.LocatorUpdate
import org.tenkiv.daqc.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device
import java.time.Duration
import java.util.concurrent.TimeoutException

abstract class DeviceLocator : Updatable<LocatorUpdate<*>> {

    /**
     * Must be thread safe, this will be accessed from multiple threads.
     */
    abstract val activeDevices: List<Device>

    abstract fun search()

    abstract fun stop()

    abstract fun awaitSpecificDevice(serialNumber: String, timeout: Duration? = null): Deferred<Device>

    abstract fun getDeviceForSerial(serialNumber: String): Device?

    protected inline fun <reified T : Device> _awaitSpecificDevice(serialNumber: String, timeout: Duration? = null):
            Deferred<T> = async(CommonPool) {
                val device = activeDevices.filter {
                    it.serialNumber == serialNumber
                }.firstOrNull() ?: awaitBroadcast(this@async, serialNumber, timeout)

                device as? T ?: throw ClassCastException(
                        "Implementation error in class extending DeviceLocator.\n" +
                                " Type parameter T passed to _awaitSpecificTekdaqc is not the type returned by the" +
                                " implementation of DeviceLocator."
                )
            }

    //TODO: Make this an inner function of _awaitSpecificDevice when it is allowed by kotlin runtime.
    suspend fun awaitBroadcast(job: CoroutineScope, serialNumber: String, timeout: Duration? = null): Device {
        val awaitingJob = broadcastChannel.openSubscription()
        val iterator = awaitingJob.iterator()

        val timeOutJob =
                if (timeout != null)
                    launch(CommonPool) {
                        delay(timeout.toMillis())
                        throw TimeoutException("Awaiting Device: $serialNumber Timed Out")
                    }
                else
                    null

        while (iterator.hasNext() && job.isActive) {
            val next = iterator.next()
            if (next is FoundDevice<*> && next.serialNumber == serialNumber) {
                awaitingJob.use {
                    timeOutJob?.cancel()
                    return next
                }
            }
        }
        throw ClosedReceiveChannelException("Broadcast channel for DeviceLocator updates was closed " +
                "while awaiting a specific device.")
    }

}