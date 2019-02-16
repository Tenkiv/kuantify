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

package org.tenkiv.kuantify.hardware.device

import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.client.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.kuantify.networking.device.*
import org.tenkiv.kuantify.networking.server.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

private val logger = KotlinLogging.logger {}

interface FSDevice : Device, NetworkConfiguredCombined {
    override fun combinedConfig(config: CombinedRouteConfig) {

    }
}

/**
 * [Device] where the corresponding [LocalDevice] DAQC is managed by Kuantify. Therefore, all [LocalDevice]s are
 * [FSBaseDevice]s but not all [RemoteDevice]s are.
 */
sealed class FSBaseDevice : FSDevice, NetworkConfiguredSide {

    //TODO Lazy thread safety mode
    internal val networkCommunicator: NetworkCommunicator by lazy(LazyThreadSafetyMode.NONE) {
        val combinedNetworkConfig = CombinedRouteConfig(this)
        combinedConfig(combinedNetworkConfig)

        val sideRouteConfig = SideRouteConfig(this)
        sideConfig(sideRouteConfig)

        val resultRouteMap = combinedNetworkConfig.networkRouteHandlerMap
        val resultUpdateChannelMap = combinedNetworkConfig.networkUpdateChannelMap

        sideRouteConfig.networkRouteHandlerMap.forEach { route, handler ->
            val currentHandler = resultRouteMap[route]
            if (currentHandler != null) {
                logger.warn { "Overriding combined route handler for route $this with side specific handler." }
            }
            resultRouteMap[route] = handler
        }

        sideRouteConfig.networkUpdateChannelMap.forEach { route, channel ->
            val currentChannel = resultUpdateChannelMap[route]
            if (currentChannel != null) {
                logger.warn { "Overriding combined route channel for route $this with side specific channel." }
            }
            resultUpdateChannelMap[route] = channel
        }

        val networkRoutHandlers = resultRouteMap.values.toList()

        NetworkCommunicator(
            this,
            networkRoutHandlers,
            resultUpdateChannelMap
        )
    }

    internal suspend fun receiveNetworkMessage(route: Route, message: String?) {
        networkCommunicator.receiveNetworkMessage(route, message)
    }

    internal abstract suspend fun sendMessage(route: Route, payload: String?)

    internal fun serializeMessage(route: Route, message: String?): String {
        return Json.stringify(NetworkMessage.serializer(), NetworkMessage(route, message))
    }
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Local Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

abstract class LocalDevice : FSBaseDevice() {

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

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

    final override suspend fun sendMessage(route: Route, payload: String?) {
        ClientHandler.sendToAll(serializeMessage(route, payload))
    }

    override fun sideConfig(config: SideRouteConfig) {

    }

    open fun getInfo(): String {
        return "null"
    }
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Remote Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

abstract class FSRemoteDevice(private val scope: CoroutineScope) : FSBaseDevice(), RemoteDevice {

    @Volatile
    private var job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext get() = scope.coroutineContext + job

    private val _isConnected = AtomicBoolean(false)
    override val isConnected: Boolean
        get() = _isConnected.get()

    internal val sendChannel = Channel<String>(10_000)

    private fun startWebsocket() {
        launch {
            httpClient.webSocket(
                method = HttpMethod.Get,
                host = "ws://$hostIp",
                port = RC.DEFAULT_PORT,
                path = RC.WEBSOCKET
            ) {
                launch {
                    sendChannel.consumeEach { message ->
                        outgoing.send(Frame.Text(message))
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
        _isConnected.set(true)
    }

    override suspend fun disconnect() {
        job.cancel()
        job = Job(scope.coroutineContext[Job])
        _isConnected.set(false)
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveMessage(message: String) {
        val (route, message) = Json.parse(NetworkMessage.serializer(), message)

        when (route.first()) {
            RC.DAQC_GATE -> networkCommunicator.receiveNetworkMessage(route, message)
            RC.MESSAGE_ERROR -> hostReportedError()
        }
    }

    private fun hostReportedError() {

    }

    final override suspend fun sendMessage(route: Route, payload: String?) {
        sendChannel.send(serializeMessage(route, payload))
    }

    override fun sideConfig(config: SideRouteConfig) {

    }

    companion object {
        suspend fun getInfo(hostIp: String): String =
            httpClient.get("${RC.HTTP}$hostIp:${RC.DEFAULT_PORT}${RC.INFO}")
    }
}