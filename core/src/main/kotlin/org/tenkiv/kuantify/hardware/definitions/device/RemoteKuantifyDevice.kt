package org.tenkiv.kuantify.hardware.definitions.device

import io.ktor.client.features.websocket.*
import io.ktor.http.*
import kotlinx.coroutines.*
import org.tenkiv.kuantify.networking.client.*

abstract class RemoteKuantifyDevice(private val scope: CoroutineScope) : RemoteDevice, CoroutineScope {

    private fun startWebsocket() {
        launch {
            httpClient.webSocket(method = HttpMethod.Get, host = hostIp, port = 80, path = "/") {

            }
        }
    }

}