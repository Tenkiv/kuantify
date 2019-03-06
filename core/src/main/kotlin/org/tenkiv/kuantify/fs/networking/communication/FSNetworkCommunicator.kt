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
import kotlinx.io.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.client.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.fs.networking.server.*
import org.tenkiv.kuantify.networking.communication.*
import org.tenkiv.kuantify.networking.configuration.*

private val logger = KotlinLogging.logger {}

private fun NetworkCommunicator<String>.buildFSRouteBindingMap(
    device: FSBaseDevice
): Map<String, NetworkRouteBinding<*, String>> {
    val combinedNetworkConfig = CombinedRouteConfig(this)
    device.combinedRouting(combinedNetworkConfig.baseRoute)

    val sideRouteConfig = SideRouteConfig(
        networkCommunicator = this,
        serializedPing = FSDevice.serializedPing,
        formatPath = ::formatPathStandard
    )
    device.sideRouting(sideRouteConfig.baseRoute)

    val resultRouteBindingMap = combinedNetworkConfig.networkRouteBindingMap

    sideRouteConfig.networkRouteBindingMap.forEach { path, binding ->
        val currentBinding = resultRouteBindingMap[path]
        if (currentBinding != null) {
            logger.warn { "Overriding combined route binding for route $path with side specific binding." }
        }
        resultRouteBindingMap[path] = binding
    }

    return resultRouteBindingMap
}

class LocalNetworkCommunicator internal constructor(
    override val device: LocalDevice
) : NetworkCommunicator<String>(device) {

    override val networkRouteBindingMap: Map<String, NetworkRouteBinding<*, String>> = buildFSRouteBindingMap(device)

    override suspend fun sendMessage(route: String, message: String) {
        ClientHandler.sendToAll(NetworkMessage(route, message).serialize())
    }

    internal fun init() {
        initBindings()
    }

    //TODO: May want to terminate all connections.
    internal fun cancel() {
        cancelCoroutines()
    }

}

class FSRemoteNetworkCommunicator internal constructor(
    override val device: FSRemoteDevice
) : NetworkCommunicator<String>(device) {

    override val networkRouteBindingMap: Map<String, NetworkRouteBinding<*, String>> = buildFSRouteBindingMap(device)

    @Volatile
    private var webSocketSession: WebSocketSession? = null

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
                    connectionStopped()
                    logger.debug { "Websocket connection closed for device: ${device.uid}" }
                }
            }
        }

        initialized.await()
    }

    override suspend fun sendMessage(route: String, message: String) {
        webSocketSession?.send(Frame.Text(NetworkMessage(route, message).serialize()))
            ?: attemptMessageWithoutConnection(route, message)
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveRawMessage(message: String) {
        val (route, message) = Json.parse(NetworkMessage.serializer(), message)

        receiveMessage(route, message)
    }

    internal suspend fun init() {
        initBindings()
        startWebsocket()
    }

    internal suspend fun cancel() {
        webSocketSession?.close()
    }

    private fun connectionStopped() {
        cancelCoroutines()
        webSocketSession = null
    }

    private fun attemptMessageWithoutConnection(route: String, message: String): Nothing {
        throw IOException(
            "Attempted to send message -$message- on route $route to device $device but there is no active connection."
        )
    }

    override fun toString(): String =
        "NetworkCommunicator for device: ${device.uid}. \nHandled network routes: ${networkRouteBindingMap.keys}"

}
