package org.tenkiv.kuantify.hardware.definitions.device

import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.client.*
import kotlin.coroutines.*

abstract class RemoteKuantifyDevice(private val scope: CoroutineScope) : KuantifyDevice(), RemoteDevice {

    @Volatile
    private var job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext get() = scope.coroutineContext + job

    internal val sendChannel = Channel<String>(10_000)

    private fun startWebsocket() {
        launch {
            httpClient.webSocket(method = HttpMethod.Get, host = hostIp, port = 80, path = "/") {
                launch {
                    sendChannel.consumeEach { message ->

                    }

                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) receiveMessage(frame.readText())
                    }
                }
            }
        }
    }

    override suspend fun connect() {
        startWebsocket()
    }

    override suspend fun disconnect() {
        job.cancel()
        job = Job(scope.coroutineContext[Job])
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveMessage(message: String) {
        val (route, message) = Json.parse(NetworkMessage.serializer(), message)

        when (route.first()) {
            Route.DAQC_GATE -> receiveDaqcGateMsg(route, message)
            Route.MESSAGE_ERROR -> hostReportedError()
        }
    }

    private fun hostReportedError() {

    }

    private fun receiveDaqcGateMsg(route: List<String>, message: String?) {
        val gateId = route.first()
        val command = route.drop(1).first()

        when (command) {

        }
    }

    private fun receiveValueMsg(gateId: String, message: String?) {

    }

}