package org.tenkiv.kuantify.networking.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import org.tenkiv.coral.*

fun Application.kuantifyHost() {
    KuantifyHost().apply { init() }
}

private class KuantifyHost {

    fun Application.init() {

        val sessionHandler = ClientHandler(this)

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

                sessionHandler.connectionOpened(clientID.id, this)


            }
        }

    }

}

internal data class ClientId(val id: String)