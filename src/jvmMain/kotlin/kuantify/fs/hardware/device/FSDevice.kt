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

package kuantify.fs.hardware.device

import io.ktor.client.request.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.*
import mu.*
import kuantify.*
import kuantify.fs.networking.*
import kuantify.fs.networking.client.*
import kuantify.fs.networking.communication.*
import kuantify.hardware.device.*
import kuantify.networking.configuration.*
import org.tenkiv.coral.Result
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

private val logger = KotlinLogging.logger {}

public interface FSDevice : Device {

    public companion object {
        @PublishedApi
        internal val serializedPing = Serialization.json.encodeToString(Unit.serializer(), Unit)
    }
}

/**
 * [Device] where the corresponding [LocalDevice] DAQC is managed by Kuantify. Therefore, all [LocalDevice]s are
 * [FSBaseDevice]s but not all [RemoteDevice]s are.
 */
public sealed class FSBaseDevice(final override val coroutineContext: CoroutineContext) : FSDevice,
    NetworkBound<String> {

    public final override val basePath: Path = emptyList()

    public override fun routing(route: NetworkRoute<String>) {

    }

}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Local Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

public abstract class LocalDevice(
    coroutineContext: CoroutineContext = GlobalScope.coroutineContext
) : FSBaseDevice(coroutineContext) {

    // Must be internal in order to be publishedApi, actually want it to be private.
    @PublishedApi
    @Volatile
    internal var communicator: LocalCommunicator? = null

    public val isHosting: Boolean
        get() = communicator?.isHosting ?: false

    public inline fun <ErrorT: Any> startHosting(
        communicationInit: (LocalDevice) -> Result<LocalCommunicator, ErrorT>
    ) : Result<Unit, ErrorT> {
        return if (!isHosting) {
            when(val result = communicationInit(this)) {
                is Result.OK -> {
                    communicator = result.value
                    Result.OK(Unit)
                }
                is Result.Failure -> result
            }
        } else {
            Result.OK(Unit)
        }
    }

    public suspend fun stopHosting() {
        communicator?.stopHosting()
        communicator = null
    }

    public open fun getInfo(): String {
        return "null"
    }
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Remote Device ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

public abstract class FSRemoteDevice protected constructor(coroutineContext: CoroutineContext) :
    FSBaseDevice(coroutineContext), RemoteDevice {

    @Volatile
    protected var networkCommunicator: FSRemoteCommunictor? = null

    private val _isConnected = AtomicBoolean(false)
    public final override val isConnected: Boolean get() = _isConnected.get()

    public final override suspend fun connect() {
        val isConnected = _isConnected.getAndSet(true)
        if (!isConnected) {
            networkCommunicator = FSRemoteWebsocketCommunicator(
                this,
                this::onCommunicatorCanceled
            ).apply {
                init()
            }
        }
    }

    public final override suspend fun disconnect() {
        onDisconnect()
    }

    private suspend fun onDisconnect() {
        networkCommunicator?.cancel()
    }

    private fun onCommunicatorCanceled() {
        _isConnected.set(false)
        networkCommunicator = null
    }

    public companion object {
        public suspend fun getInfo(hostIp: String): String =
            httpClient.get<String>("${RC.HTTP}$hostIp:${RC.DEFAULT_PORT}${RC.INFO}").also {
                logger.trace { "Got info for device at IP address $hostIp" }
            }
    }
}
