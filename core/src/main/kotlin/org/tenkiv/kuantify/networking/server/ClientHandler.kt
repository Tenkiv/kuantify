package org.tenkiv.kuantify.networking.server

import io.ktor.http.cio.websocket.*

// Must only be accessed from Daqc dispatcher.
internal object ClientHandler {

    private val clients: MutableMap<String, HostedClient> = HashMap()

    fun connectionOpened(clientId: String, session: WebSocketSession) {
        if (clients.containsKey(clientId)) {
            clients[clientId]?.addSession(session)
        } else {
            clients[clientId] = HostedClient(clientId).apply {
                addSession(session)
            }
        }
    }

    fun connectionClosed(clientId: String, session: WebSocketSession) {
        clients[clientId]?.removeSession(session, clients)
    }

    //TODO: Instead of always sending to all clients, register clients with HostDeviceCommunicators
    suspend fun sendToAll(message: String) {
        clients.values.forEach {
            it.sendMessage(message)
        }
    }

}

internal class HostedClient(val id: String) {

    private val websocketSessions: MutableList<WebSocketSession> = ArrayList()

    fun addSession(session: WebSocketSession) {
        websocketSessions += session
    }

    fun removeSession(session: WebSocketSession, clients: MutableMap<String, HostedClient>) {
        websocketSessions -= session
        if (websocketSessions.isEmpty()) clients -= id
    }

    suspend fun sendMessage(message: String) {
        websocketSessions.forEach { it.send(Frame.Text(message)) }
    }

}