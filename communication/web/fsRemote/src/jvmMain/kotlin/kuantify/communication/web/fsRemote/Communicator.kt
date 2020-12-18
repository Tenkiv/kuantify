/*
 * Copyright 2020 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kuantify.communication.web.fsRemote

import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kuantify.*
import kuantify.fs.hardware.device.*
import kuantify.fs.networking.*
import kuantify.fs.networking.communication.*
import kuantify.networking.*
import kuantify.networking.communication.*
import mu.*
import org.tenkiv.coral.*
import java.io.*
import kotlin.time.*

private val logger = KotlinLogging.logger {}

private data class WebCommunicatorInitializer(
    val hostIP: String,
    val port: UInt16,
    val msgResponseTimeout: Duration
) : FSRemoteCommsInitializer {

    override suspend fun init(
        device: FSRemoteDevice,
        timeout: Duration
    ): FSRemoteCommsInitResult = FSRemoteWebsocketCommunicator(device).init(hostIP, port, timeout, msgResponseTimeout)

}

/**
 * @param connectTimeout Amount of time to wait for connection to host be successfully made.
 * @param msgResponseTimeout Amount of time to wait for ping response once active connection has been established.
 */
public suspend fun FSRemoteDevice.connectWeb(
    hostIP: String,
    port: UInt16 = RC.DEFAULT_PORT,
    connectTimeout: Duration = 15.seconds,
    msgResponseTimeout: Duration = 5.seconds
): Result<Unit, RemoteCommsInitErr> =
    connect(connectTimeout, WebCommunicatorInitializer(hostIP, port, msgResponseTimeout))

internal class FSRemoteWebsocketCommunicator(
    device: FSRemoteDevice
) : FSRemoteCommunictor(device) {

    override val isConnected: Boolean
        get() = webSocketSession != null

    @Volatile
    private var webSocketSession: WebSocketSession? = null

    public override val communicationMode: CommunicationMode
        get() = CommunicationMode.NON_EXCLUSIVE

    private val fullyClosed = CompletableDeferred<Unit>()

    private suspend fun startWebsocket(
        hostIP: String,
        port: UInt16,
        connectTimeout: Duration,
        msgResponseTimeout: Duration
    ): Result<Unit, RemoteCommsInitErr> {
        val initialized = CompletableDeferred<Unit>()

        launch {
            httpClient.ws(
                method = HttpMethod.Get,
                host = hostIP,
                port = port.toInt32(),
                path = RC.WEBSOCKET
            ) {
                pingIntervalMillis = msgResponseTimeout.toLongMilliseconds()
                timeoutMillis = msgResponseTimeout.toLongMilliseconds()

                webSocketSession = this
                logger.debug { "Websocket connection opened for device: ${device.uid}" }

                initialized.complete(Unit)
                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            receiveRawMessage(frame.readText())
                            logger.trace {
                                "Received message - ${frame.readText()} - from remote device: ${device.uid}"
                            }
                        }
                    }
                } finally {
                    val closeReason = closeReason.await()
                    if (closeReason?.code != CloseReason.Codes.NORMAL.code) {
                        alertCriticalError(
                            CriticalDaqcError.TerminalConnectionDisruption(
                                device,
                                "Websocket connection closed abnormally."
                            )
                        )
                    }
                    logger.debug {
                        "Websocket connection closed for device: ${device.uid}, reason: $closeReason"
                    }
                    connectionStopped()
                }
            }
        }

        // If there was a timeout in attempting to make the connection.
        return if (withTimeoutOrNull(connectTimeout) { initialized.await() } == null) {
            Result.Failure(RemoteCommsInitErr.Timeout())
        } else {
            Result.OK(Unit)
        }

    }

    public override suspend fun sendMessage(route: String, message: String) {
        webSocketSession?.send(Frame.Text(FSNetworkMessage(route, message).serialize()))?.also {
            logger.trace { "Sent on route: $route, message - $message - to remote device: ${device.uid}" }
        } ?: attemptMessageWithoutConnection(route, message)
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveRawMessage(message: String) {
        val (route, message) = KuantifySerialization.json.decodeFromString(FSNetworkMessage.serializer(), message)

        receiveMessage(route, message)
    }

    internal suspend fun init(
        hostIP: String,
        port: UInt16,
        connectTimeout: Duration,
        msgResponseTimeout: Duration
    ): FSRemoteCommsInitResult {
        initBindings()
        return startWebsocket(hostIP, port, connectTimeout, msgResponseTimeout).map { this }
    }

    override suspend fun close() {
        webSocketSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Explicit disconnect by remote."))
        fullyClosed.await()
    }

    private fun connectionStopped() {
        cancel()
        notifyClosed()
        fullyClosed.complete(Unit)
        webSocketSession = null
    }

    private fun attemptMessageWithoutConnection(route: String, message: String): Nothing {
        throw IOException(
            "Attempted to send message -$message- on route $route to device $device but there is no active connection."
        )
    }

}