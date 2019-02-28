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
 *
 */

package org.tenkiv.kuantify.networking.device

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.device.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

typealias UpdateReceiver = suspend (update: String?) -> Unit
typealias MessageSerializer<T> = (update: T) -> String

internal sealed class NetworkRouteBinding<T>(
    protected val device: FSBaseDevice,
    val networkUpdateChannel: Channel<String?>?
) : CoroutineScope {

    final override val coroutineContext: CoroutineContext
        get() = device.coroutineContext + job

    @Volatile
    protected var job = Job(device.coroutineContext[Job])

    open fun start(job: Job) {
        this.job = job
    }

    internal class Host<T>(
        device: FSBaseDevice,
        private val route: String,
        private val localUpdateChannel: ReceiveChannel<T>?,
        networkUpdateChannel: Channel<String?>?,
        private val serializeMessage: MessageSerializer<T>?,
        private val sendUpdatesFromHost: Boolean,
        private val receiveUpdateOnHost: UpdateReceiver?
    ) : NetworkRouteBinding<T>(device, networkUpdateChannel) {

        override fun start(job: Job) {
            super.start(job)
            // Send
            if (sendUpdatesFromHost) {
                launch {
                    localUpdateChannel?.consumeEach {
                        val payload = serializeMessage?.invoke(it)
                        device.sendMessage(route, payload)
                    } ?: TODO("Throw specific exception")
                }
            }

            // Receive
            if (receiveUpdateOnHost != null) {
                launch {
                    networkUpdateChannel?.consumeEach {
                        receiveUpdateOnHost.invoke(it)
                    } ?: TODO("Throw specific exception")
                }
            }
        }
    }

    internal class Remote<T>(
        device: FSBaseDevice,
        private val route: String,
        private val localUpdateChannel: ReceiveChannel<T>?,
        networkUpdateChannel: Channel<String?>?,
        private val serializeMessage: MessageSerializer<T>?,
        private val sendUpdatesFromRemote: Boolean,
        private val receiveUpdateOnRemote: UpdateReceiver?,
        private val isFullyBiDirectional: Boolean
    ) : NetworkRouteBinding<T>(device, networkUpdateChannel) {

        private val ignoreNextUpdate = AtomicBoolean(false)

        override fun start(job: Job) {
            super.start(job)
            // Send
            if (sendUpdatesFromRemote) {
                if (isFullyBiDirectional) {
                    launch {
                        localUpdateChannel?.consumeEach {
                            if (!ignoreNextUpdate.get()) {
                                val payload = serializeMessage?.invoke(it)
                                device.sendMessage(route, payload)
                            } else {
                                ignoreNextUpdate.set(false)
                            }
                        } ?: TODO("Throw specific exception")
                    }
                } else {
                    launch {
                        localUpdateChannel?.consumeEach {
                            val payload = serializeMessage?.invoke(it)
                            device.sendMessage(route, payload)
                        } ?: TODO("Throw specific exception")
                    }
                }
            }

            // Receive
            if (receiveUpdateOnRemote != null) {
                if (isFullyBiDirectional) {
                    launch {
                        networkUpdateChannel?.consumeEach {
                            ignoreNextUpdate.set(true)
                            receiveUpdateOnRemote.invoke(it)
                        } ?: TODO("Throw specific exception")
                    }
                } else {
                    launch {
                        networkUpdateChannel?.consumeEach {
                            receiveUpdateOnRemote.invoke(it)
                        } ?: TODO("Throw specific exception")
                    }
                }
            }
        }

    }

}
