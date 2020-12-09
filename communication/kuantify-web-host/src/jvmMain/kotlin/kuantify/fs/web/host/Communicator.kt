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

package kuantify.fs.web.host

import kotlinx.coroutines.*
import kuantify.fs.hardware.device.*
import kuantify.fs.networking.communication.*
import org.tenkiv.coral.*

public suspend fun LocalDevice.startHostingWeb(): Result<Unit, WebHostInitErr> = startHosting(
    object : LocalCommsInitializer<WebHostInitErr> {
        override suspend fun init(device: LocalDevice): Result<LocalCommunicator, WebHostInitErr> =
            if (Host.ktorAppRunning) {
                val communicator = LocalWebsocketCommunicator(device).apply { init() }
                Host.startHosting(communicator)
                Result.OK(communicator)
            } else {
                Result.Failure(WebHostInitErr.KTOR_APP_NOT_RUNNING)
            }
    }
)

public enum class WebHostInitErr(public val description: String) {
    KTOR_APP_NOT_RUNNING(
        """
            Cannot start hosting using web communicator until the ktor server is started with the kuantifyHost module.
        """.trimToSingleLine()
    )
}

internal class LocalWebsocketCommunicator(device: LocalDevice) : LocalCommunicator(device) {

    override val isHosting: Boolean
        get() = Host.isHosting && isActive

    override suspend fun sendMessage(route: String, message: String) {
        Host.sendMessage(route, message)
    }

    fun init() {
        initBindings()
    }

    override suspend fun close() {
        Host.stopHosting()
        cancel()
    }

}