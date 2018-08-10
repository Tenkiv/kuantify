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