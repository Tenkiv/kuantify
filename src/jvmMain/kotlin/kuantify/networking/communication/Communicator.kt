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

package kuantify.networking.communication

import kotlinx.coroutines.*
import mu.*
import kuantify.*
import kuantify.hardware.device.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*
import kotlinx.coroutines.cancel as cancelCoroutineScope

private val logger = KotlinLogging.logger {}

public abstract class Communicator<SerialT>(
    device: Device
) : CoroutineScope {
    protected val job: Job = Job(device.coroutineContext[Job])

    public final override val coroutineContext: CoroutineContext = device.coroutineContext + job

    private val bindingsInitialized = AtomicBoolean(false)

    protected abstract val networkRouteBindingMap: Map<String, NetworkRouteBinding<SerialT>>

    public abstract val device: Device

    protected fun initBindings() {
        //TODO: This needs to use compareAndSet
        if (!bindingsInitialized.get()) {
            networkRouteBindingMap.values.forEach { it.start() }
            bindingsInitialized.set(true)
            logger.trace { "Initialized bindings for NetworkCommunicator: $this" }
        } else {
            logger.debug { "Attempted to initialize - $this - multiple times." }
        }
    }

    protected fun cancelCoroutines() {
        cancelCoroutineScope()
    }

    /**
     * Cleanly release all associated resources and kill this communicator such that it can never be used again.
     */
    @KuantifyComponentBuilder
    public abstract suspend fun cancel()

    @KuantifyComponentBuilder
    public suspend fun receiveMessage(route: String, message: SerialT) {
        networkRouteBindingMap[route]?.messageFromNetwork(message) ?: unboundRouteError(route, message)
    }

    @KuantifyComponentBuilder
    public abstract suspend fun sendMessage(route: String, message: SerialT)

    private suspend fun unboundRouteError(route: String, message: SerialT) {
        logger.error { "Received message - $message - for unbound route: $route." }
        alertCriticalError( CriticalDaqcError.FailedMajorCommand(
            device,
            "Route not defined for received message."
        ))
    }

    public override fun toString(): String =
        """"NetworkCommunicator for device: ${device.uid}.
            |RouteBindings: ${networkRouteBindingMap.values}
            |job=$job""".trimMargin()

}

public abstract class RemoteCommunicator<SerialT>(device: Device) : Communicator<SerialT>(device) {

    public abstract val communicationMode: CommunicationMode

}

public enum class CommunicationMode {
    /**
     * Means other devices can also be connected to the host.
     */
    NON_EXCLUSIVE
}