/*
 * Copyright 2019 Tenkiv, Inc.
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

package org.tenkiv.kuantify.networking.communication

import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.hardware.device.*
import kotlin.coroutines.*

@PublishedApi
internal inline fun remoteDeviceCommand(device: RemoteDevice, logger: KLogger, op: () -> Unit) =
    if (device.isConnected.value) {
        op()
        true
    } else {
        val errorMsg = "Attempted to send command to device $device but there is no connection to the device."

        criticalDaqcErrorBroadcaster.offer(
            CriticalDaqcError.FailedMajorCommand(
                device,
                errorMsg
            )
        )
        logger.error { errorMsg }
        false
    }

public fun <T> RemoteDevice.RemoteUpdatable(): Updatable<T> = RemoteUpdatable(device = this)

public fun <T> RemoteDevice.RemoteUpdatable(initialValue: T): InitializedUpdatable<T> =
    InitializedRemoteUpdatable(this, initialValue)

private class RemoteUpdatable<T>(private val device: RemoteDevice) : Updatable<T> {

    override val coroutineContext: CoroutineContext get() = device.coroutineContext

    private val _updateBroadcaster = ConflatedBroadcastChannel<T>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out T> get() = _updateBroadcaster

    override fun set(value: T) {
        remoteDeviceCommand(device, logger) {
            _updateBroadcaster.offer(value)
        }
    }

    companion object : KLogging()
}

private class InitializedRemoteUpdatable<T>(private val device: RemoteDevice, initialValue: T) :
    InitializedUpdatable<T> {

    override val coroutineContext: CoroutineContext get() = device.coroutineContext

    private val _updateBroadcaster = ConflatedBroadcastChannel(initialValue)
    override val updateBroadcaster: ConflatedBroadcastChannel<out T> get() = _updateBroadcaster

    override var value: T
        get() = updateBroadcaster.value
        set(value) = set(value)

    override fun set(value: T) {
        remoteDeviceCommand(device, logger) {
            _updateBroadcaster.offer(value)
        }
    }

    companion object : KLogging()
}