package org.tenkiv.kuantify.networking.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.Route

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

                val clientID = call.sessions.get<ClientId>()

                if (clientID == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                    return@webSocket
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

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveMessage(clientId: String, message: String) {
        val (route, message) = Json.parse(NetworkMessage.serializer(), message)
        when {
            route.first() == Route.DEVICE -> HostedDeviceManager.receiveMessage(route.drop(1), message)
        }
    }

}

internal data class ClientId(val id: String)