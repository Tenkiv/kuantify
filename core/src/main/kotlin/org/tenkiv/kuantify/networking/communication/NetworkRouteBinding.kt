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

package org.tenkiv.kuantify.networking.communication

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.device.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

typealias UpdateReceiver<ST> = suspend (update: ST) -> Unit
typealias MessageSerializer<MT, ST> = (update: MT) -> ST

sealed class NetworkRouteBinding<MT, ST>(
    protected val device: NetworkableDevice<ST>,
    val networkUpdateChannel: Channel<ST>?
) : CoroutineScope {

    final override val coroutineContext: CoroutineContext
        get() = device.coroutineContext + job

    @Volatile
    protected var job = Job(device.coroutineContext[Job])

    open fun start(job: Job) {
        this.job = job
    }

    class Host<MT, ST>(
        device: NetworkableDevice<ST>,
        private val route: String,
        private val localUpdateChannel: ReceiveChannel<MT>?,
        networkUpdateChannel: Channel<ST>?,
        private val serializeMessage: MessageSerializer<MT, ST>?,
        private val sendUpdatesFromHost: Boolean,
        private val receiveUpdateOnHost: UpdateReceiver<ST>?,
        private val serializedPing: ST
    ) : NetworkRouteBinding<MT, ST>(device, networkUpdateChannel) {

        override fun start(job: Job) {
            super.start(job)
            // Send
            if (sendUpdatesFromHost) {
                launch {
                    localUpdateChannel?.consumeEach {
                        val message = serializeMessage?.invoke(it) ?: serializedPing
                        device.networkCommunicator._sendMessage(route, message)
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

    class Remote<MT, ST>(
        device: NetworkableDevice<ST>,
        private val route: String,
        private val localUpdateChannel: ReceiveChannel<MT>?,
        networkUpdateChannel: Channel<ST>?,
        private val serializeMessage: MessageSerializer<MT, ST>?,
        private val sendUpdatesFromRemote: Boolean,
        private val receiveUpdateOnRemote: UpdateReceiver<ST>?,
        private val isFullyBiDirectional: Boolean,
        private val serializedPing: ST
    ) : NetworkRouteBinding<MT, ST>(device, networkUpdateChannel) {

        private val ignoreNextUpdate = AtomicBoolean(false)

        override fun start(job: Job) {
            super.start(job)
            // Send
            if (sendUpdatesFromRemote) {
                if (isFullyBiDirectional) {
                    launch {
                        localUpdateChannel?.consumeEach {
                            if (!ignoreNextUpdate.get()) {
                                val message = serializeMessage?.invoke(it) ?: serializedPing
                                device.networkCommunicator._sendMessage(route, message)
                            } else {
                                ignoreNextUpdate.set(false)
                            }
                        } ?: TODO("Throw specific exception")
                    }
                } else {
                    launch {
                        localUpdateChannel?.consumeEach {
                            val message = serializeMessage?.invoke(it) ?: serializedPing
                            device.networkCommunicator._sendMessage(route, message)
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
