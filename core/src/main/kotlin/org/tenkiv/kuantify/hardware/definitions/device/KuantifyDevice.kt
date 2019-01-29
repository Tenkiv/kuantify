package org.tenkiv.kuantify.hardware.definitions.device

import kotlinx.coroutines.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.kuantify.networking.device.*
import org.tenkiv.kuantify.networking.server.*
import kotlin.coroutines.*

/**
 * [Device] where the corresponding [LocalDevice] DAQC is managed by Kuantify. Therefore, all [LocalDevice]s are
 * [KuantifyDevice]s but not all [RemoteDevice]s are.
 */
sealed class KuantifyDevice : Device {

    internal val networkCommunicator: NetworkCommunicator = run {
        val networkCommunicatorBuilder = NetworkCommunicatorBuilder(this)
        networkCommunicatorBuilder.configureNetworking()

        NetworkCommunicator(
            this,
            networkCommunicatorBuilder.networkRouteHandlers,
            networkCommunicatorBuilder.networkUpdateChannelMap
        )
    }

    internal suspend fun receiveNetworkMessage(route: Route, message: String?) {
        networkCommunicator.receiveNetworkMessage(route, message)
    }

    internal abstract fun sendMessage(route: Route, payload: String?)

    protected abstract fun NetworkCommunicatorBuilder.configureNetworking()
}

abstract class LocalDevice : KuantifyDevice() {

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

}