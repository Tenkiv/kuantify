package org.tenkiv.kuantify.hardware.definitions.device

import io.ktor.client.features.websocket.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.client.*
import kotlin.coroutines.*

abstract class RemoteKuantifyDevice(private val scope: CoroutineScope) : RemoteDevice {

    @Volatile
    private var job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext get() = scope.coroutineContext + job

    private val sendChannel = Channel<NetworkMessage>(10_000)

    private fun startWebsocket() {
        launch {
            httpClient.webSocket(method = HttpMethod.Get, host = hostIp, port = 80, path = "/") {
                launch {
                    sendChannel.consumeEach { message ->

                    }

                    incoming.consumeEach { frame ->

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
}