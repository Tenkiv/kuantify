package org.tenkiv.kuantify.networking.device

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.definitions.device.*

typealias Route = List<String>

internal class NetworkCommunicator(
    val device: KuantifyDevice,
    private val networkRouteHandlers: List<NetworkRouteHandler<*, *>>,
    private val networkUpdateChannelMap: Map<Route, Channel<String?>>
) {

    private val parentJob: Job? = device.coroutineContext[Job]

    @Volatile
    private var job: Job = Job(parentJob)

    fun start() {
        networkRouteHandlers.forEach { it.start(job) }
    }

    fun stop() {
        job.cancel()
        job = Job(parentJob)
    }

    suspend fun receiveNetworkMessage(route: Route, payload: String?) {
        networkUpdateChannelMap[route]?.send(payload) ?: TODO("handle invalid route")
    }

}
