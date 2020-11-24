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

package kuantify.fs.web.host

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
import kuantify.*
import kuantify.fs.hardware.device.*
import kuantify.fs.networking.*
import kuantify.fs.web.host.*
import kuantify.lib.*
import mu.*
import kotlin.time.*

private val logger = KotlinLogging.logger {}

/**
 * Kuantify websocket host module for ktor application.
 */
public fun Application.kuantifyHost() {
    Host.apply { init() }
}

internal data class ClientId(val id: String)

internal object Host {

    //TODO: Make const
    private val ILLEGAL_KTOR_SHUTDOWN = """Cannot stop ktor server while this device is actively functioning as a
        |Kuantify host via web communicator. Stop hosting device before stopping ktor server.
    """.trimMargin()

    @Volatile
    private var hostCommunicator: LocalWebsocketCommunicator? = null

    private val hostedDevice: LocalDevice? get() = hostCommunicator?.device

    internal val isHosting get() = hostCommunicator != null

    @Volatile
    internal var ktorAppRunning: Boolean = false

    fun Application.init() {
        install(DefaultHeaders)
        install(WebSockets)
        install(Sessions) {
            cookie<ClientId>("CLIENT_ID")
        }
        intercept(ApplicationCallPipeline.Features) {
            if (call.sessions.get<ClientId>() == null) {
                call.sessions.set(ClientId(generateNonce()))
            }
        }

        environment.monitor.subscribe(ApplicationStarted) {
            ktorAppRunning = true
        }

        environment.monitor.subscribe(ApplicationStopPreparing) {
            ktorAppRunning = false
            // Hosting must be stopped explicitly from the LocalDevice. Ktor application stopping without first stopping
            // hosting of the LocalDevice would cause hosting to end silently and potentially unexpectedly even if we
            // ensured the correct shutdown steps were taken here.
            if (isHosting) throw IllegalStateException(ILLEGAL_KTOR_SHUTDOWN)
        }

        routing {
            get(RC.INFO) {
                //TODO: We probably want to be more specific than just returning null string when the device is not hosted.
                // Maybe always return a JSON serialized sealed class with NoInfo, NotHosted, and Info(String) variants.
                call.respondText(hostedDevice?.getInfo() ?: "null")
                logger.trace { "Sent info for local device" }
            }

            webSocket(path = RC.WEBSOCKET) {
                // If this device is not hosting
                if (hostCommunicator == null) {
                    // Connection is not allowed, immediately close the connection.
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Not hosting"))
                    logger.warn { "Attempted websocket connection while device not hosted." }
                    return@webSocket
                }
                // If this device is hosting
                // Initiate normal websocket connection
                timeout = 15.seconds.toJavaDuration()

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

    fun startHosting(communicator: LocalWebsocketCommunicator) {
        hostCommunicator = communicator
        logger.debug { "Started hosting local device" }
    }

    suspend fun stopHosting() {
        ClientHandler.closeAllSessions()
        logger.debug { "Stopped hosting local device" }
        hostCommunicator = null
    }

    suspend fun sendMessage(route: String, message: String) {
        ClientHandler.sendToAll(FSNetworkMessage(route, message).serialize())
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveMessage(clientId: ClientId, message: String) {
        val (route, message) = KuantifySerialization.json.decodeFromString(FSNetworkMessage.serializer(), message)

        hostCommunicator?.receiveMessage(route, message) ?: deviceNotHosted(clientId, message)
    }

    private fun deviceNotHosted(clientId: ClientId, message: String) {
        logger.warn { "Received message - $message - from client: $clientId but there is no device being hosted." }
    }

}

private object ClientHandler {

    private val mutexClients: MutexValue<MutableMap<ClientId, Client>> = MutexValue(HashMap(), Mutex())

    suspend fun connectionOpened(clientId: ClientId, session: WebSocketSession) {
        mutexClients.withLock { clients ->
            if (clients.containsKey(clientId)) {
                clients[clientId]?.addSession(session)
            } else {
                clients[clientId] = Client(clientId).apply {
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

    private class Client(val id: ClientId) {

        //TODO: We may want to only allow 1 session per client.
        private val websocketSessions: MutableList<WebSocketSession> = ArrayList()

        fun addSession(session: WebSocketSession) {
            websocketSessions += session
        }

        fun removeSession(session: WebSocketSession, clients: MutableMap<ClientId, Client>) {
            websocketSessions -= session
            if (websocketSessions.isEmpty()) clients -= id
        }

        suspend fun sendMessage(serializedMsg: String) {
            websocketSessions.forEach { it.send(Frame.Text(serializedMsg)) }
        }

        suspend fun closeAllSessions() {
            websocketSessions.forEach {
                it.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Local device stopped hosting."))
            }
        }

    }

}