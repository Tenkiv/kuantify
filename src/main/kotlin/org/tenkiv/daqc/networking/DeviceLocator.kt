package org.tenkiv.daqc.networking

import arrow.core.Success
import arrow.core.Try
import arrow.core.getOrElse
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.withTimeout
import org.tenkiv.daqc.Updatable
import org.tenkiv.daqc.hardware.definitions.device.Device
import java.time.Duration
import java.util.concurrent.TimeUnit

abstract class DeviceLocator<T : Device> : Updatable<LocatorUpdate<T>> {

    /**
     * Must be thread safe, this will be read from multiple threads.
     */
    abstract val activeDevices: List<T>

    abstract fun search()

    abstract fun stop()

    /**
     * @throws [ClosedReceiveChannelException] if the locator was stopped before the specific device could be found.
     */
    @Throws(ClosedReceiveChannelException::class)
    suspend fun getDeviceForSerial(serialNumber: String): T {
        return activeDevices.firstOrNull {
            it.serialNumber == serialNumber
        } ?: awaitBroadcast(serialNumber).getOrElse { throw it }
    }

    suspend fun tryGetDeviceForSerial(serialNumber: String, timeout: Duration? = null): Try<T> {
        val fromMemory = activeDevices.firstOrNull {
            it.serialNumber == serialNumber
        }

        return if (fromMemory != null) {
            Success(fromMemory)
        } else {
            awaitBroadcast(serialNumber, timeout)
        }
    }

    fun getDeviceForSerialOrNull(serialNumber: String): T? =
        activeDevices.firstOrNull { it.serialNumber == serialNumber }

    @Suppress("MemberVisibilityCanBePrivate") // it's a lie, PublishedApi must be internal
    @PublishedApi
    internal suspend fun awaitBroadcast(
        serialNumber: String,
        timeout: Duration? = null
    ): Try<T> = Try {
        val subscription = broadcastChannel.openSubscription()

        suspend fun getDevice(): T {
            subscription.consumeEach {
                if (it is FoundDevice<*> && it.serialNumber == serialNumber) return it.wrappedDevice
            }

            throw ClosedReceiveChannelException(
                "Broadcast channel for DeviceLocator updates was closed " +
                        "while awaiting a specific device."
            )
        }

        if (timeout != null) {
            withTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS) {
                getDevice()
            }
        } else {
            getDevice()
        }

    }

}

sealed class LocatorUpdate<out T : Device>(val wrappedDevice: T) : Device by wrappedDevice
/**
 * A Device found by a Locator
 */
class FoundDevice<out T : Device>(device: T) : LocatorUpdate<T>(device) {

    override fun toString() = "FoundDevice: $wrappedDevice"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FoundDevice<*>

        if (wrappedDevice != other.wrappedDevice) return false

        return true
    }

    override fun hashCode(): Int {
        return wrappedDevice.hashCode()
    }
}

class LostDevice<out T : Device>(device: T) : LocatorUpdate<T>(device) {

    override fun toString() = "LostDevice: $wrappedDevice"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LostDevice<*>

        if (wrappedDevice != other.wrappedDevice) return false

        return true
    }

    override fun hashCode(): Int {
        return wrappedDevice.hashCode()
    }


}