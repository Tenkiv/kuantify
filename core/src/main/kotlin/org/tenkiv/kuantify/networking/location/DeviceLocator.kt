/*
 * Copyright 2018 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.kuantify.networking.location

import arrow.core.Success
import arrow.core.Try
import arrow.core.getOrElse
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.withTimeout
import org.tenkiv.kuantify.Updatable
import org.tenkiv.kuantify.hardware.definitions.device.Device
import java.time.Duration

/**
 * Class defining functions expected from Device Locators.
 */
abstract class DeviceLocator<T : Device> : Updatable<LocatorUpdate<T>> {

    /**
     * The list of active devices that are both found and able to be connected to.
     *
     * Must be thread safe, this will be read from multiple threads.
     */
    abstract val activeDevices: List<T>

    /**
     * Begins the active search for devices.
     */
    abstract fun search()

    /**
     * Stops the active search for devices.
     */
    abstract fun stop()

    /**
     * Gets the device with the specified serial number either from the known active list or suspends and awaits
     * discovery of the device.
     *
     * @param serialNumber The serial number of the device.
     * @return The device with specified serial number.
     * @throws [ClosedReceiveChannelException] If the locator was stopped before the specific device could be found.
     */
    @Throws(ClosedReceiveChannelException::class)
    suspend fun getDeviceForSerial(serialNumber: String): T {
        return activeDevices.firstOrNull {
            it.uid == serialNumber
        } ?: awaitBroadcast(serialNumber).getOrElse { throw it }
    }

    /**
     * Gets the device with the specified serial number either from the known active list or suspends and awaits
     * discovery of the device.
     *
     * @param serialNumber The serial number of the device.
     * @return A [Try] of a [Device] or an exception.
     * @throws [ClosedReceiveChannelException] If the locator was stopped before the specific device could be found.
     */
    suspend fun tryGetDeviceForSerial(serialNumber: String, timeout: Duration? = null): Try<T> {
        val fromMemory = activeDevices.firstOrNull {
            it.uid == serialNumber
        }

        return if (fromMemory != null) {
            Success(fromMemory)
        } else {
            awaitBroadcast(serialNumber, timeout)
        }
    }

    /**
     * Gets the device with the specified serial number if it has been found. If not it returns null.
     *
     * @param serialNumber The serial number of the device.
     * @retun The device with the specifeid serial number or null.
     */
    fun getDeviceForSerialOrNull(serialNumber: String): T? =
        activeDevices.firstOrNull { it.uid == serialNumber }

    @Suppress("MemberVisibilityCanBePrivate") // it's a lie, PublishedApi must be internal
    @PublishedApi
    internal suspend fun awaitBroadcast(
        serialNumber: String,
        timeout: Duration? = null
    ): Try<T> = Try {
        val subscription = broadcastChannel.openSubscription()

        suspend fun getDevice(): T {
            subscription.consumeEach {
                if (it is FoundDevice<*> && it.uid == serialNumber) return it.wrappedDevice
            }

            throw ClosedReceiveChannelException(
                "Broadcast channel for DeviceLocator updates was closed " +
                        "while awaiting a specific device."
            )
        }

        if (timeout != null) {
            withTimeout(timeout.toMillis()) {
                getDevice()
            }
        } else {
            getDevice()
        }

    }
}

/**
 * Sealed class for notifications to changes in the status of Devices.
 *
 * @param wrappedDevice The base device to be wrapped.
 */
sealed class LocatorUpdate<out T : Device>(val wrappedDevice: T) : Device by wrappedDevice
/**
 * A Device found by a Locator
 *
 * @param device The located [Device].
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
/**
 * A Device previously found by a Locator that is no longer able to be connected to.
 *
 * @param device The removed [Device].
 */
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