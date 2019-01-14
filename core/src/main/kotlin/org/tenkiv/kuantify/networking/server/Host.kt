package org.tenkiv.kuantify.networking.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*

fun Application.kuantifyHost() {
    KuantifyHost().apply { init() }
}

private class KuantifyHost {

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
            webSocket("/") {
                withContext(Dispatchers.Daqc) {
                    val clientID = call.sessions.get<ClientId>()

                    if (clientID == null) {
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                        return@withContext
                    }

                    ClientHandler.connectionOpened(clientID.id, this@webSocket)

                    try {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) receiveMessage(clientID.id, frame.readText())
                        }
                    } finally {
                        ClientHandler.connectionClosed(clientID.id, this@webSocket)
                    }
                }
            }
        }

    }

    private suspend fun receiveMessage(clientId: String, message: String) {
        when {
            message.startsWith(DEVICE_CMD) -> // Send to host device communicator
        }
    }

    companion object {
        const val DEVICE_CMD = "/device"
    }

}

internal data class ClientId(val id: String)