/*
 * Copyright 2019 Tenkiv, Inc.
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
 *
 */

package org.tenkiv.kuantify.fs.hardware.device

import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.kuantify.fs.hardware.device.FSDevice.Companion.SERIALIZED_PING
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.client.*
import org.tenkiv.kuantify.fs.networking.communication.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.fs.networking.server.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.networking.communication.*
import org.tenkiv.kuantify.networking.configuration.*
import kotlin.coroutines.*

private val logger = KotlinLogging.logger {}

interface FSDevice : NetworkableDevice<String>, NetworkBoundCombined {
    override fun combinedRouting(routing: CombinedNetworkRouting) {

    }

    companion object {
        internal const val SERIALIZED_PING = ""
    }
}

/**
 * [Device] where the corresponding [LocalDevice] DAQC is managed by Kuantify. Therefore, all [LocalDevice]s are
 * [FSBaseDevice]s but not all [RemoteDevice]s are.
 */
sealed class FSBaseDevice : FSDevice, NetworkBoundSide<String> {

    final override val basePath: Path = emptyList()

    internal fun buildRouteBindingMap(): Map<String, NetworkRouteBinding<*, String>> {
        val combinedNetworkConfig = CombinedRouteConfig(this)
        combinedRouting(combinedNetworkConfig.baseRoute)

        val sideRouteConfig =
            SideRouteConfig(this, SERIALIZED_PING) { it.toPathString() }
        sideRouting(sideRouteConfig.baseRoute)

        val resultRouteBindingMap = combinedNetworkConfig.networkRouteBindingMap

        sideRouteConfig.networkRouteBindingMap.forEach { path, binding ->
            val currentHandler = resultRouteBindingMap[path]
            if (currentHandler != null) {
                logger.warn { "Overriding combined route binding for route $path with side specific binding." }
            }
            resultRouteBindingMap[path] = binding
        }

        return resultRouteBindingMap
    }

    internal suspend fun receiveNetworkMessage(route: String, message: String) {
        networkCommunicator.receiveMessage(route, message)
    }

    override fun sideRouting(routing: SideNetworkRouting<String>) {

    }


}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Local Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

abstract class LocalDevice : FSBaseDevice() {

    final override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    //TODO: Thread safety
    final override val networkCommunicator: LocalNetworkCommunicator by lazy(LazyThreadSafetyMode.NONE) {
        LocalNetworkCommunicator(this, buildRouteBindingMap())
    }

    val isHosting: Boolean
        get() = KuantifyHost.isHosting

    fun startHosting() {
        networkCommunicator.start()
        KuantifyHost.startHosting(this)
    }

    suspend fun stopHosting() {
        KuantifyHost.stopHosting()
        networkCommunicator.stop()
    }

    open fun getInfo(): String {
        return "null"
    }
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Remote Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

abstract class FSRemoteDevice(private val scope: CoroutineScope) : FSBaseDevice(), RemoteDevice<String> {

    final override val coroutineContext: CoroutineContext get() = scope.coroutineContext

    final override val networkCommunicator: FSRemoteNetworkCommunicator by lazy(LazyThreadSafetyMode.NONE) {
        FSRemoteNetworkCommunicator(this, buildRouteBindingMap())
    }

    @Volatile
    private var webSocketSession: WebSocketSession? = null

    override val isConnected: Boolean
        get() = webSocketSession != null

    private suspend fun runWebsocket() {
        val initialized = CompletableDeferred<Boolean>()

        launch {
            httpClient.ws(
                method = HttpMethod.Get,
                host = hostIp,
                port = RC.DEFAULT_PORT,
                path = RC.WEBSOCKET
            ) {
                webSocketSession = this
                logger.debug { "Websocket connection opened for device: ${this@FSRemoteDevice.uid}" }

                initialized.complete(true)
                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            receiveMessage(frame.readText())
                            logger.trace { "Received message - ${frame.readText()} - from remote device: $uid" }
                        }
                    }
                } finally {
                    // TODO: Connection closed.
                    logger.debug { "Websocket connection closed for device: ${this@FSRemoteDevice.uid}" }
                }
            }
        }

        initialized.await()
    }


    override suspend fun connect() {
        networkCommunicator.start()
        logger.debug { "Network communicator started for device: ${this.uid}, now running websocket" }
        runWebsocket()
    }

    override suspend fun disconnect() {
        webSocketSession?.close()
        networkCommunicator.stop()
        webSocketSession = null
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveMessage(message: String) {
        val (route, message) = Json.parse(NetworkMessage.serializer(), message)

        networkCommunicator.receiveMessage(route, message)
    }

    suspend fun sendMessage(route: String, message: String) {
        webSocketSession?.send(Frame.Text(NetworkMessage(route, message).serialize()))
            ?: TODO("Throw specific exception")
    }

    companion object {
        suspend fun getInfo(hostIp: String): String =
            httpClient.get<String>("${RC.HTTP}$hostIp:${RC.DEFAULT_PORT}${RC.INFO}").also {
                logger.trace { "Got info for device at IP address $hostIp" }
            }
    }
}