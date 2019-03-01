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
import org.tenkiv.kuantify.hardware.device.*

abstract class NetworkCommunicator<ST>(
    device: NetworkableDevice<ST>,
    protected val networkRouteBindingMap: Map<String, NetworkRouteBinding<*, ST>>
) {

    private val parentJob: Job? = device.coroutineContext[Job]

    protected abstract val device: NetworkableDevice<ST>

    @Volatile
    private var job: Job = Job(parentJob)

    protected open fun startImpl() {
        networkRouteBindingMap.values.forEach { it.start(job) }
    }

    protected open fun stopImpl() {
        job.cancel()
        job = Job(parentJob)
    }

    internal suspend fun receiveMessage(route: String, message: ST) {
        networkRouteBindingMap[route]?.networkUpdateChannel?.send(message) ?: TODO("handle invalid route")
    }

    protected abstract suspend fun sendMessage(route: String, message: ST)

    internal suspend fun _sendMessage(route: String, message: ST) = sendMessage(route, message)

    override fun toString(): String =
        "NetworkCommunicator for device: ${device.uid}. \nHandled network routes: ${networkRouteBindingMap.keys}"
}