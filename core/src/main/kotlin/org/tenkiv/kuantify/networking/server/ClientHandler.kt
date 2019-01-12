package org.tenkiv.kuantify.networking.server

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*

// Only one instance of this should ever be created. Use as singleton.
internal class ClientHandler(scope: CoroutineScope) : CoroutineScope {

    override val coroutineContext: CoroutineContext = scope.coroutineContext

    private val clientsActor = actor<Msg> {
        val sessions: MutableMap<String, HostedClient> = HashMap()


    }


    suspend fun connectionOpened(clientId: String, websocket: WebSocketSession) =
        clientsActor.send(Msg.ConnectionOpened(clientId, websocket))

    private suspend fun _connectionOpened(clientId: String, websocket: WebSocketSession) {

    }

    suspend fun connectionClosed(clientId: String, websocket: WebSocketSession) =
        clientsActor.send(Msg.ConnectionClosed(clientId, websocket))

    private suspend fun _connectionClosed(clientId: String, websocket: WebSocketSession) {

    }

    sealed class Msg {
        data class ConnectionOpened(val clientId: String, val websocket: WebSocketSession) : Msg()
        data class ConnectionClosed(val clientId: String, val websocket: WebSocketSession) : Msg()
    }

}

internal class HostedClient {

    val websockets: List<WebSocketSession> = ArrayList()


    sealed class Msg {

    }
}