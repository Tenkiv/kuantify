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
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.kuantify.fs.hardware.device.FSDevice.Companion.serializedPing
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
        internal val serializedPing = Json.stringify(UnitSerializer, Unit)
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

        val sideRouteConfig = SideRouteConfig(
            device = this,
            serializedPing = serializedPing,
            formatPath = ::formatPathStandard
        )
        sideRouting(sideRouteConfig.baseRoute)

        val resultRouteBindingMap = combinedNetworkConfig.networkRouteBindingMap

        sideRouteConfig.networkRouteBindingMap.forEach { path, binding ->
            val currentBinding = resultRouteBindingMap[path]
            if (currentBinding != null) {
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

abstract class FSRemoteDevice(final override val coroutineContext: CoroutineContext) : FSBaseDevice(),
    RemoteDevice<String> {

    final override val networkCommunicator: FSRemoteNetworkCommunicator by lazy(LazyThreadSafetyMode.NONE) {
        FSRemoteNetworkCommunicator(this, buildRouteBindingMap())
    }

    override val isConnected: Boolean
        get() = networkCommunicator.isConnected


    override suspend fun connect() {
        networkCommunicator.startConnection()
    }

    override suspend fun disconnect() {
        networkCommunicator.stopConnection()
    }

    companion object {
        suspend fun getInfo(hostIp: String): String =
            httpClient.get<String>("${RC.HTTP}$hostIp:${RC.DEFAULT_PORT}${RC.INFO}").also {
                logger.trace { "Got info for device at IP address $hostIp" }
            }
    }
}