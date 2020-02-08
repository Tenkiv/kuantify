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

package org.tenkiv.kuantify.networking.communication

import kotlinx.coroutines.*
import mu.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.hardware.device.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

private val logger = KotlinLogging.logger {}

public abstract class NetworkCommunicator<ST>(
    device: Device
) : CoroutineScope {

    protected val job = Job(device.coroutineContext[Job])

    public final override val coroutineContext: CoroutineContext = device.coroutineContext + job

    private val bindingsInitialized = AtomicBoolean(false)

    protected abstract val networkRouteBindingMap: Map<String, NetworkRouteBinding<*, ST>>

    public abstract val device: Device

    protected fun initBindings() {
        if (!bindingsInitialized.get()) {
            networkRouteBindingMap.values.forEach { it.start() }
            bindingsInitialized.set(true)
            logger.trace { "Initialized bindings for NetworkCommunicator: $this" }
        } else {
            logger.debug { "Attempted to initialize - $this - multiple times." }
        }
    }

    protected fun cancelCoroutines() {
        job.cancel()
    }

    internal suspend fun receiveMessage(route: String, message: ST) {
        networkRouteBindingMap[route]?.networkUpdateChannel?.send(message) ?: unboundRouteMessage(route, message)
    }

    protected abstract suspend fun sendMessage(route: String, message: ST)

    internal suspend fun _sendMessage(route: String, message: ST) = sendMessage(route, message)

    private fun unboundRouteMessage(route: String, message: ST) {
        criticalDaqcErrorBroadcaster.offer(
            CriticalDaqcError.FailedMajorCommand(
                device,
                "Route not defined for received message."
            )
        )
        logger.error { "Received message - $message - for unbound route: $route." }
    }

    public override fun toString(): String =
        """"NetworkCommunicator for device: ${device.uid}.
            |RouteBindings: ${networkRouteBindingMap.values}
            |job=$job""".trimMargin()

}

public abstract class RemoteNetworkCommunicator<ST>(device: Device) : NetworkCommunicator<ST>(device) {

    public abstract val communicationMode: CommunicationMode

}

public enum class CommunicationMode {
    NO_CONNECTION,
    /**
     * Means other devices can also be connected to the host.
     */
    NON_EXCLUSIVE
}