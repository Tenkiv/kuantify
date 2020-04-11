/*
 * Copyright 2020 Tenkiv, Inc.
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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.trackable.*

@PublishedApi
internal val _privateRemoteUtilsLogger = KotlinLogging.logger {}

@PublishedApi
internal inline fun remoteDeviceCommand(device: RemoteDevice, op: () -> Unit) =
    if (device.isConnected.value) {
        op()
        true
    } else {
        val errorMsg = "Attempted to send command to device $device but there is no connection to the device."
        //TODO: Is this ok?
        GlobalScope.launch(Dispatchers.Unconfined) {
            alertCriticalError(CriticalDaqcError.FailedMajorCommand(device, errorMsg))
        }
        _privateRemoteUtilsLogger.error { errorMsg }
        false
    }

public fun <T: Any> RemoteDevice.RemoteUpdatable(): Updatable<T> = RemoteUpdatable(device = this)

public fun <T: Any> RemoteDevice.RemoteUpdatable(initialValue: T): InitializedUpdatable<T> =
    InitializedRemoteUpdatable(this, initialValue)

private class RemoteUpdatable<T: Any>(private val device: RemoteDevice) : Updatable<T> {
    override val valueOrNull: T?
        get() = broadcastChannel.valueOrNull

    private val broadcastChannel = ConflatedBroadcastChannel<T>()

    override fun set(value: T) {
        remoteDeviceCommand(device) {
            broadcastChannel.offer(value)
        }
    }

    override fun openSubscription(): ReceiveChannel<T> = broadcastChannel.openSubscription()

}

private class InitializedRemoteUpdatable<T : Any>(private val device: RemoteDevice, initialValue: T) :
    InitializedUpdatable<T> {
    override var value: T
        get() = broadcastChannel.value
        set(value) = set(value)

    private val broadcastChannel = ConflatedBroadcastChannel(initialValue)

    override fun openSubscription(): ReceiveChannel<T> = broadcastChannel.openSubscription()

    override fun set(value: T) {
        remoteDeviceCommand(device) {
            broadcastChannel.offer(value)
        }
    }

}