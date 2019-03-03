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

import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.client.*
import org.tenkiv.kuantify.fs.networking.server.*
import org.tenkiv.kuantify.networking.communication.*

private val logger = KotlinLogging.logger {}

class LocalNetworkCommunicator internal constructor(
    override val device: LocalDevice,
    networkRouteBindingMap: Map<String, NetworkRouteBinding<*, String>>
) : NetworkCommunicator<String>(device.coroutineContext, networkRouteBindingMap) {

    override suspend fun sendMessage(route: String, message: String) {
        ClientHandler.sendToAll(NetworkMessage(route, message).serialize())
    }

    internal fun start() {
        startBindings()
    }

    internal fun stop() {
        stopBindings()
    }

}

class FSRemoteNetworkCommunicator internal constructor(
    override val device: FSRemoteDevice,
    networkRouteBindingMap: Map<String, NetworkRouteBinding<*, String>>
) : NetworkCommunicator<String>(device.coroutineContext, networkRouteBindingMap) {

    @Volatile
    private var webSocketSession: WebSocketSession? = null

    internal val isConnected: Boolean
        get() = webSocketSession != null

    private suspend fun startWebsocket() {
        val initialized = CompletableDeferred<Boolean>()

        launch {
            httpClient.ws(
                method = HttpMethod.Get,
                host = device.hostIp,
                port = RC.DEFAULT_PORT,
                path = RC.WEBSOCKET
            ) {
                webSocketSession = this
                logger.debug { "Websocket connection opened for device: ${device.uid}" }

                initialized.complete(true)
                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            receiveRawMessage(frame.readText())
                            logger.trace {
                                "Received message - ${frame.readText()} - from remote device: ${device.uid}"
                            }
                        }
                    }
                } finally {
                    // TODO: Connection closed.
                    logger.debug { "Websocket connection closed for device: ${device.uid}" }
                }
            }
        }

        initialized.await()
    }

    override suspend fun sendMessage(route: String, message: String) {
        webSocketSession?.send(Frame.Text(NetworkMessage(route, message).serialize()))
            ?: TODO("Throw specific exception")
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveRawMessage(message: String) {
        val (route, message) = Json.parse(NetworkMessage.serializer(), message)

        receiveMessage(route, message)
    }

    internal suspend fun startConnection() {
        startBindings()
        startWebsocket()
        logger.debug { "Network communicator started for device: ${device.uid}." }
    }

    internal suspend fun stopConnection() {
        webSocketSession?.close()
        stopBindings()
        webSocketSession = null
    }

}
