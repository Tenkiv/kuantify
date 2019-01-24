package org.tenkiv.kuantify.networking.server

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*

internal object ClientHandler {

    private val mutexClients: MutexValue<MutableMap<String, HostedClient>> = MutexValue(HashMap(), Mutex())

    suspend fun connectionOpened(clientId: String, session: WebSocketSession) {
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

    suspend fun connectionClosed(clientId: String, session: WebSocketSession) {
        mutexClients.withLock { clients ->
            clients[clientId]?.removeSession(session, clients)
        }
    }

    //TODO: Instead of always sending to all clients, register clients with HostDeviceCommunicators
    suspend fun sendToAll(route: List<String>, value: String?) {
        val message = NetworkMessage(route, value)
        val serializedMsg = Json.stringify(NetworkMessage.serializer(), message)

        mutexClients.withLock { clients ->
            clients.values.forEach {
                it.sendMessage(serializedMsg)
            }
        }
    }

}

internal class HostedClient(val id: String) {

    private val mutexWebsocketSessions: MutexValue<MutableList<WebSocketSession>> = MutexValue(ArrayList(), Mutex())

    suspend fun addSession(session: WebSocketSession) {
        mutexWebsocketSessions.withLock { websocketSessions ->
            websocketSessions += session
        }
    }

    suspend fun removeSession(session: WebSocketSession, clients: MutableMap<String, HostedClient>) {
        mutexWebsocketSessions.withLock { websocketSessions ->
            websocketSessions -= session
            if (websocketSessions.isEmpty()) clients -= id
        }
    }

    suspend fun sendMessage(serializedMsg: String) {
        mutexWebsocketSessions.withLock { websocketSessions ->
            websocketSessions.forEach { it.send(Frame.Text(serializedMsg)) }
        }
    }

}