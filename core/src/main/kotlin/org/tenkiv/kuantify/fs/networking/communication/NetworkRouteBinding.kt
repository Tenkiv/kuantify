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

package org.tenkiv.kuantify.fs.networking.communication

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.networking.communication.*

class LocalDeviceRouteBinding<MT, ST>(
    communicator: NetworkCommunicator<ST>,
    private val route: String,
    private val localUpdateChannel: ReceiveChannel<MT>?,
    networkUpdateChannel: Channel<ST>?,
    private val serializeMessage: MessageSerializer<MT, ST>?,
    private val sendUpdatesFromHost: Boolean,
    private val receiveUpdateOnHost: UpdateReceiver<ST>?,
    private val serializedPing: ST
) : NetworkRouteBinding<MT, ST>(communicator, networkUpdateChannel) {

    override fun start(bindingJob: Job) {
        // Send
        if (sendUpdatesFromHost) {
            launch(bindingJob) {
                localUpdateChannel?.consumeEach {
                    val message = serializeMessage?.invoke(it) ?: serializedPing
                    communicator._sendMessage(route, message)
                } ?: throwIllegalStateSend(route, communicator.device)
            }
        }

        // Receive
        if (receiveUpdateOnHost != null) {
            launch(bindingJob) {
                networkUpdateChannel?.consumeEach {
                    receiveUpdateOnHost.invoke(it)
                } ?: throwIllegalStateReceive(route, communicator.device)
            }
        }
    }
}