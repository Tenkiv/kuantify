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

package org.tenkiv.kuantify.networking.communication

import kotlinx.coroutines.*
import mu.*
import kotlin.coroutines.*

private val logger = KotlinLogging.logger {}

abstract class NetworkCommunicator<ST>(
    final override val coroutineContext: CoroutineContext,
    protected val networkRouteBindingMap: Map<String, NetworkRouteBinding<*, ST>>
) : CoroutineScope {

    private val parentJob: Job? get() = coroutineContext[Job]

    @Volatile
    private var bindingJob: Job = Job(parentJob)

    protected fun startBindings() {
        networkRouteBindingMap.values.forEach { it.start(bindingJob) }
    }

    protected fun stopBindings() {
        bindingJob.cancel()
        bindingJob = Job(parentJob)
    }

    internal suspend fun receiveMessage(route: String, message: ST) {
        networkRouteBindingMap[route]?.networkUpdateChannel?.send(message) ?: unboundRouteMessage(route, message)
    }

    protected abstract suspend fun sendMessage(route: String, message: ST)

    internal suspend fun _sendMessage(route: String, message: ST) = sendMessage(route, message)

    private fun newBindingJob() {
        bindingJob = Job(parentJob)
    }

    private fun unboundRouteMessage(route: String, message: ST) {
        logger.debug { "Received message - $message - for unbound route: $route." }
    }
}