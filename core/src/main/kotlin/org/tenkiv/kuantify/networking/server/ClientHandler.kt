package org.tenkiv.kuantify.networking.server

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.sync.*
import org.tenkiv.kuantify.lib.*

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

    suspend fun sendToAll(message: String) {
        mutexClients.withLock { clients ->
            clients.values.forEach {
                it.sendMessage(message)
            }
        }
    }

    suspend fun closeAllSessions() {
        mutexClients.withLock { clients ->
            clients.values.forEach { it.closeAllSessions() }
        }
    }

    private class HostedClient(val id: String) {

        private val websocketSessions: MutableList<WebSocketSession> = ArrayList()

        fun addSession(session: WebSocketSession) {
            websocketSessions += session
        }

        fun removeSession(session: WebSocketSession, clients: MutableMap<String, HostedClient>) {
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