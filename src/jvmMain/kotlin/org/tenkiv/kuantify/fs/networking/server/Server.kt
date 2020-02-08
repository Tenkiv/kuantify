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

package org.tenkiv.kuantify.fs.networking.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.lib.*

private val logger = KotlinLogging.logger {}

public fun Application.kuantifyHost() {
    KuantifyHost.apply { init() }
}

internal data class ClientId(val id: String)

internal object KuantifyHost {

    @Volatile
    private var hostedDevice: LocalDevice? = null

    internal val isHosting get() = hostedDevice != null

    fun Application.init() {

        install(DefaultHeaders)

        install(WebSockets) {
            pingPeriod = 1.minutesSpan
        }

        install(Sessions) {
            cookie<ClientId>("CLIENT_ID")
        }

        intercept(ApplicationCallPipeline.Features) {
            if (call.sessions.get<ClientId>() == null) {
                call.sessions.set(ClientId(generateNonce()))
            }
        }

        routing {
            get(RC.INFO) {
                call.respondText(hostedDevice?.getInfo() ?: "null")
                logger.trace { "Sent info for local device" }
            }

            webSocket(RC.WEBSOCKET) {

                val clientID = call.sessions.get<ClientId>()

                logger.debug { "Websocket connection opened for client $clientID" }

                if (clientID == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                    return@webSocket
                }

                ClientHandler.connectionOpened(clientID, this@webSocket)

                logger.debug { "Starting websocket receive loop" }

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            receiveMessage(clientID, frame.readText())
                            logger.trace {
                                "Received message - ${frame.readText()} - on local device ${hostedDevice?.uid}"
                            }
                        }
                    }
                } finally {
                    ClientHandler.connectionClosed(clientID, this@webSocket)
                    logger.debug(
                        "Websocket connection closed for client ${clientID.id}, reason: ${closeReason.await()}."
                    )
                }

            }
        }

    }

    internal fun startHosting(device: LocalDevice) {
        hostedDevice = device
        logger.debug { "Started hosting local device" }
    }

    internal suspend fun stopHosting() {
        ClientHandler.closeAllSessions()
        logger.debug { "Stopped hosting local device" }
        hostedDevice = null
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveMessage(clientId: ClientId, message: String) {
        val (route, message) = Serialization.json.parse(NetworkMessage.serializer(), message)

        hostedDevice?.receiveNetworkMessage(route, message) ?: deviceNotHosted(clientId, message)
    }

    private fun deviceNotHosted(clientId: ClientId, message: String) {
        logger.debug { "Received message - $message - from client: $clientId but there is no device being hosted." }
    }

}

internal object ClientHandler {

    private val mutexClients: MutexValue<MutableMap<ClientId, HostedClient>> = MutexValue(HashMap(), Mutex())

    suspend fun connectionOpened(clientId: ClientId, session: WebSocketSession) {
        mutexClients.withLock { clients ->
            if (clients.containsKey(clientId)) {
                clients[clientId]?.addSession(session)
            } else {
                clients[clientId] = HostedClient(clientId).apply {
                    addSession(session)
                }
            }
        }
    }

    suspend fun connectionClosed(clientId: ClientId, session: WebSocketSession) {
        mutexClients.withLock { clients ->
            clients[clientId]?.removeSession(session, clients)
        }
    }

    suspend fun sendToAll(message: String) {
        mutexClients.withLock { clients ->
            clients.values.forEach {
                it.sendMessage(message)
            }
            logger.trace { "Sent message - $message - from local device" }
        }
    }

    suspend fun closeAllSessions() {
        mutexClients.withLock { clients ->
            clients.values.forEach { it.closeAllSessions() }
        }
    }

    private class HostedClient(val id: ClientId) {

        private val websocketSessions: MutableList<WebSocketSession> = ArrayList()

        fun addSession(session: WebSocketSession) {
            websocketSessions += session
        }

        fun removeSession(session: WebSocketSession, clients: MutableMap<ClientId, HostedClient>) {
            websocketSessions -= session
            if (websocketSessions.isEmpty()) clients -= id
        }

        suspend fun sendMessage(serializedMsg: String) {
            websocketSessions.forEach { it.send(Frame.Text(serializedMsg)) }
        }

        suspend fun closeAllSessions() {
            websocketSessions.forEach { it.close() }
        }

    }

}