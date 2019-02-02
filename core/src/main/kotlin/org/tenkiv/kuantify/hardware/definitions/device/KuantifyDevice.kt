package org.tenkiv.kuantify.hardware.definitions.device

import io.ktor.client.features.websocket.*
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
import kotlin.coroutines.*

private val logger = KotlinLogging.logger {}

interface KuantifyDevice : Device {

    fun CombinedRouteConfig.combinedRouteConfig() {}

}

/**
 * [Device] where the corresponding [LocalDevice] DAQC is managed by Kuantify. Therefore, all [LocalDevice]s are
 * [BaseKuantifyDevice]s but not all [RemoteDevice]s are.
 */
sealed class BaseKuantifyDevice : KuantifyDevice {

    internal val networkCommunicator: NetworkCommunicator = run {
        val combinedNetworkConfig = CombinedRouteConfig(this)
        combinedNetworkConfig.combinedRouteConfig()

        val sideRouteConfig = SideRouteConfig(this)
        sideRouteConfig.sideRouteConfig()

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

    internal abstract fun sendMessage(route: Route, payload: String?)

    internal abstract fun <D : BaseKuantifyDevice> SideRouteConfig<D>.sideRouteConfig()

}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Local Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

abstract class LocalDevice : BaseKuantifyDevice() {

    @Volatile
    private var job = Job()

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext + job

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

    protected open fun <D : BaseKuantifyDevice> SideRouteConfig<D>.localRouteConfig() {}

    internal override fun <D : BaseKuantifyDevice> SideRouteConfig<D>.sideRouteConfig() {
        localRouteConfig()
    }
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Remote Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

abstract class RemoteKuantifyDevice(private val scope: CoroutineScope) : BaseKuantifyDevice(), RemoteDevice {

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
            RC.DAQC_GATE -> networkCommunicator.receiveNetworkMessage(route, message)
            RC.MESSAGE_ERROR -> hostReportedError()
        }
    }

    private fun hostReportedError() {

    }

    protected open fun <D : BaseKuantifyDevice> SideRouteConfig<D>.remoteRouteConfig() {}

    internal override fun <D : BaseKuantifyDevice> SideRouteConfig<D>.sideRouteConfig() {
        remoteRouteConfig()
    }

}