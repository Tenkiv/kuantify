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
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.*

fun Application.kuantifyHost() {
    KuantifyHost.apply { init() }
}

internal object KuantifyHost {

    @Volatile
    private var hostedDevice: LocalDevice? = null

    internal val isHosting = hostedDevice != null

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

    internal fun startHosting(device: LocalDevice) {
        hostedDevice = device
    }

    internal suspend fun stopHosting() {
        ClientHandler.closeAllSessions()
        hostedDevice = null
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveMessage(clientId: String, message: String) {
        val (route, message) = Json.parse(NetworkMessage.serializer(), message)
        when (route.first()) {
            RC.MESSAGE_ERROR -> clientReportedError()
            else -> hostedDevice?.receiveNetworkMessage(route, message) ?: deviceNotHosted()
        }
    }

    private fun deviceNotHosted() {

    }

    private fun clientReportedError() {

    }

}

internal data class ClientId(val id: String)