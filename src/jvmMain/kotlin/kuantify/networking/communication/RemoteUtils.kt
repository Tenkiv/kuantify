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

package kuantify.networking.communication

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import mu.*
import kuantify.*
import kuantify.hardware.device.*
import kuantify.trackable.*

@PublishedApi
internal val _privateRemoteUtilsLogger = KotlinLogging.logger {}

@KuantifyComponentBuilder
public inline fun remoteDeviceCommand(device: RemoteDevice, op: () -> Unit): Boolean =
    if (device.isConnected) {
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

@KuantifyComponentBuilder
public fun <T : Any> RemoteDevice.RemoteUpdatable(): Updatable<T> = RemoteUpdatable(device = this)

private class RemoteUpdatable<T : Any>(private val device: RemoteDevice) : Updatable<T> {
    override val valueOrNull: T?
        get() = _flow.replayCache.firstOrNull()

    private val _flow = MutableSharedFlow<T>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val flow: SharedFlow<T> get() = _flow

    override fun set(value: T) {
        remoteDeviceCommand(device) {
            _flow.tryEmit(value)
        }
    }

}

/**
 * An [Updatable] meant to keep sync both ways between a remote client and a host on the remote side.
 * It's value will not be updated immediately when set, only once it receives confirmation of the updated from the host.
 *
 * [set] acts as a [Device] command so it will cause a critical error if set is attempted when there is no
 * connection.
 */
@KuantifyComponentBuilder
public interface RemoteSyncUpdatable<T : Any> : Updatable<T> {
    public val localSetChannel: Channel<T>

    /**
     * Updates actually updates the value of this Updatable to the value received from the host.
     */
    public fun update(value: T)
}

private class RemoteSyncUpdatableImpl<T : Any>(private val device: RemoteDevice) : RemoteSyncUpdatable<T> {
    override val valueOrNull: T?
        get() = _flow.replayCache.firstOrNull()

    private val _flow = MutableSharedFlow<T>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val flow: SharedFlow<T> get() = _flow
    override val localSetChannel: Channel<T> = Channel(capacity = Channel.CONFLATED)

    /**
     * Sends the new value over the network to the host.
     */
    override fun set(value: T) {
        remoteDeviceCommand(device) {
            localSetChannel.offer(value)
        }
    }

    /**
     * Updates the value of this Updatable to the value received from the host.
     */
    override fun update(value: T) {
        _flow.tryEmit(value)
    }

}

private class CustomSetRemoteSyncUpdatable<T : Any>(
    private val device: RemoteDevice,
    private val customSetter: UpdatableSetter<T>
) : RemoteSyncUpdatable<T> {
    override val valueOrNull: T?
        get() = _flow.replayCache.firstOrNull()

    private val _flow = MutableSharedFlow<T>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val flow: SharedFlow<T> get() = _flow
    override val localSetChannel: Channel<T> = Channel(capacity = Channel.CONFLATED)

    private val setValue = Updatable.ValueSetter<T> { value -> localSetChannel.offer(value) }

    /**
     * Sends the new value over the network to the host.
     */
    override fun set(value: T) {
        remoteDeviceCommand(device) {
            setValue.customSetter(value)
        }
    }

    /**
     * Updates the value of this Updatable to the value received from the host.
     */
    override fun update(value: T) {
        _flow.tryEmit(value)
    }

}

@KuantifyComponentBuilder
public fun <T : Any> RemoteDevice.RemoteSyncUpdatable(): RemoteSyncUpdatable<T> = RemoteSyncUpdatableImpl(this)

@KuantifyComponentBuilder
public fun <T : Any> RemoteDevice.RemoteSyncUpdatable(setter: UpdatableSetter<T>): RemoteSyncUpdatable<T> =
    CustomSetRemoteSyncUpdatable(this, setter)
