package org.tenkiv.kuantify.hardware.definitions.device

import kotlinx.coroutines.*
import org.tenkiv.kuantify.networking.property.handler.*
import org.tenkiv.kuantify.networking.server.*
import kotlin.coroutines.*

/**
 * [Device] where the corresponding [LocalDevice] DAQC is managed by Kuantify. Therefore, all [LocalDevice]s are
 * [KuantifyDevice]s but not all [RemoteDevice]s are.
 */
sealed class KuantifyDevice : Device {

    protected abstract val networkCommunicator: NetworkCommunicator

    internal suspend fun receiveMessage(route: Route, message: String?) {
        networkCommunicator.receiveMessage(route, message)
    }

    internal abstract suspend fun sendMessage(route: Route, serializedValue: String?)

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