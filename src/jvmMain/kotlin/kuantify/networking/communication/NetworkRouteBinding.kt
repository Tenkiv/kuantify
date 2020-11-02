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
import kuantify.lib.*
import kuantify.networking.configuration.*

private val logger = KotlinLogging.logger {}

//TODO: Should this be sealed class?
@KuantifyComponentBuilder
public interface NetworkRouteBinding<SerialT : Any> {

    public fun start()

    public suspend fun messageFromNetwork(message: SerialT)

}

internal class MessageBinding<BoundT, SerialT : Any>(
    private val communicator: Communicator<SerialT>,
    private val route: String,
    private val messageSender: MessageSender<BoundT, SerialT>?,
    private val messageReceiver: MessageReceiver<SerialT>?
) : NetworkRouteBinding<SerialT>, CoroutineScope by communicator {

    public override fun start() {
        // Send
        if (messageSender != null) {
            launch {
                messageSender.onEachMessage {
                    val message = messageSender.serialize.invoke(it)
                    communicator.sendMessage(route, message)
                }
            }
        }

        // Receive
        if (messageReceiver != null) {
            launch {
                messageReceiver.channel.consumingOnEach {
                    messageReceiver.receiveOp(it)
                }
            }
        }
    }

    public override suspend fun messageFromNetwork(message: SerialT) {
        messageReceiver?.channel?.send(message) ?: cantReceiveError(message)
    }

    private suspend fun cantReceiveError(message: SerialT) {
        logger.error { "Received message - $message - on route: $route with no receive functionality." }
        alertCriticalError( CriticalDaqcError.FailedMajorCommand(
            communicator.device,
            "Unable to receive on route of incoming message."
        ))
    }

    public override fun toString(): String = "NetworkMessageBinding(route=$route)"

}

internal class PingBinding<SerialT : Any>(
    private val communicator: Communicator<SerialT>,
    private val route: String,
    private val localUpdateChannel: ReceiveChannel<Ping>?,
    private val pingReceiver: PingReceiver?,
    private val serializedPing: SerialT
) : NetworkRouteBinding<SerialT>, CoroutineScope by communicator {

    public override fun start() {
        // Send
        if (localUpdateChannel != null) {
            launch {
                localUpdateChannel.consumingOnEach {
                    communicator.sendMessage(route, serializedPing)
                }
            }
        }

        // Receive
        if (pingReceiver != null) {
            launch {
                pingReceiver.channel.consumingOnEach {
                    pingReceiver.receiveOp()
                }
            }
        }
    }

    public override suspend fun messageFromNetwork(message: SerialT) {
        pingReceiver?.channel?.offer(Ping) ?: cantReceiveError()
    }

    private suspend fun cantReceiveError() {
        logger.error { "Received ping on route: $route with no receive functionality." }
        alertCriticalError( CriticalDaqcError.FailedMajorCommand(
            communicator.device,
            "Unable to receive on route of incoming ping."
        ))
    }

    public override fun toString(): String = "NetworkPingBinding(route=$route)"
}