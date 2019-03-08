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

import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.io.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.client.*
import org.tenkiv.kuantify.fs.networking.communication.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.fs.networking.server.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.networking.configuration.*
import kotlin.coroutines.*
import kotlin.properties.*
import kotlin.reflect.*

private val logger = KotlinLogging.logger {}

interface FSDevice : Device, NetworkBoundCombined {
    override fun combinedRouting(routing: CombinedNetworkRouting) {

    }

    companion object {
        internal val serializedPing = Json.stringify(UnitSerializer, Unit)
    }
}

/**
 * [Device] where the corresponding [LocalDevice] DAQC is managed by Kuantify. Therefore, all [LocalDevice]s are
 * [FSBaseDevice]s but not all [RemoteDevice]s are.
 */
sealed class FSBaseDevice : FSDevice, NetworkBoundSide<String> {

    final override val basePath: Path = emptyList()

    override fun sideRouting(routing: SideNetworkRouting<String>) {

    }

}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Local Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

abstract class LocalDevice : FSBaseDevice() {

    final override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    @Volatile
    private var networkCommunicator: LocalNetworkCommunicator? = null

    val isHosting: Boolean
        get() = KuantifyHost.isHosting && networkCommunicator?.isActive == true

    fun startHosting() {
        if (!isHosting) {
            networkCommunicator = LocalNetworkCommunicator(this).apply { init() }
            KuantifyHost.startHosting(this)
        }
    }

    suspend fun stopHosting() {
        KuantifyHost.stopHosting()
        networkCommunicator?.cancel()
        networkCommunicator = null
    }

    internal suspend fun receiveNetworkMessage(route: String, message: String) {
        networkCommunicator?.receiveMessage(route, message)
            ?: throw IOException("Attempted to receive message without communicator.")
    }

    open fun getInfo(): String {
        return "null"
    }
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Remote Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

abstract class FSRemoteDevice protected constructor(final override val coroutineContext: CoroutineContext) :
    FSBaseDevice(), RemoteDevice {

    /**
     * Implementation should delegate using networkCommunicator function.
     */
    protected abstract var networkCommunicator: FSRemoteNetworkCommunictor

    private val _isConnected = Updatable(false)
    override val isConnected: InitializedTrackable<Boolean> get() = _isConnected

    override suspend fun connect() {
        val noConnectionCommunicator = networkCommunicator
        if (!isConnected.value && noConnectionCommunicator is FSRemoteNoConnectionCommunicator) {
            networkCommunicator = FSRemoteWebsocketCommunicator(
                this,
                this::onCommunicatorCanceled
            ).apply {
                init()
                noConnectionCommunicator.immediateCancel()
            }
            _isConnected.value = true
        }
    }

    override suspend fun disconnect() {
        onDisconnect()
    }

    private suspend fun onDisconnect() {
        networkCommunicator.cancel()
    }

    private fun onCommunicatorCanceled() {
        _isConnected.value = false
        networkCommunicator = FSRemoteNoConnectionCommunicator(this)
    }

    protected fun networkCommunicator(): FSRemoteCommunicatorDelegate = FSRemoteCommunicatorDelegate(this)

    companion object {
        suspend fun getInfo(hostIp: String): String =
            httpClient.get<String>("${RC.HTTP}$hostIp:${RC.DEFAULT_PORT}${RC.INFO}").also {
                logger.trace { "Got info for device at IP address $hostIp" }
            }
    }
}

class FSRemoteCommunicatorDelegate internal constructor(device: FSRemoteDevice) :
    ReadWriteProperty<FSRemoteDevice, FSRemoteNetworkCommunictor> {

    @Volatile
    private var networkCommunicator: FSRemoteNetworkCommunictor = FSRemoteNoConnectionCommunicator(device)

    override fun getValue(thisRef: FSRemoteDevice, property: KProperty<*>): FSRemoteNetworkCommunictor {
        return networkCommunicator
    }

    override fun setValue(thisRef: FSRemoteDevice, property: KProperty<*>, value: FSRemoteNetworkCommunictor) {
        networkCommunicator = value
    }

}