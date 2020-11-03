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

import kotlinx.coroutines.*
import kotlinx.serialization.builtins.*
import kuantify.*
import kuantify.fs.networking.*
import kuantify.fs.networking.communication.*
import kuantify.hardware.device.*
import kuantify.networking.*
import kuantify.networking.configuration.*
import mu.*
import org.tenkiv.coral.*
import kotlin.coroutines.*
import kotlin.time.*

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

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Local Device ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
//TODO: Make functional interface if possible in the future.
@KuantifyComponentBuilder
public interface LocalCommsInitializer<ErrorT : Any> {

    public suspend fun init(device: LocalDevice): Result<LocalCommunicator, ErrorT>

}

public abstract class LocalDevice(
    coroutineContext: CoroutineContext = GlobalScope.coroutineContext
) : FSBaseDevice(coroutineContext) {

    // Must be internal in order to be publishedApi, actually want it to be private.
    @PublishedApi
    @Volatile
    internal var communicator: LocalCommunicator? = null

    public val isHosting: Boolean
        get() = communicator?.isHosting ?: false

    @KuantifyComponentBuilder
    public suspend fun <ErrorT : Any> startHosting(
        communicatorInitializer: LocalCommsInitializer<ErrorT>
    ): Result<Unit, ErrorT> {
        return if (!isHosting) {
            when (val result = communicatorInitializer.init(this)) {
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
        communicator?.close()
        communicator = null
    }

    public open fun getInfo(): String {
        return "null"
    }
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Remote Device ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
//TODO: Make functional interface if possible in the future.
@KuantifyComponentBuilder
public interface FSRemoteCommsInitializer {

    public suspend fun init(
        device: FSRemoteDevice,
        timeout: Duration
    ): FSRemoteCommsInitResult

}

public abstract class FSRemoteDevice protected constructor(coroutineContext: CoroutineContext) :
    FSBaseDevice(coroutineContext), RemoteDevice {

    @Volatile
    private var communicatorInitializer: FSRemoteCommsInitializer? = null

    @Volatile
    private var communicator: FSRemoteCommunictor? = null

    public final override val isConnected: Boolean get() = communicator?.isConnected ?: false

    /**
     * @param timeout sets timeout for making connection.
     */
    @KuantifyComponentBuilder
    public suspend fun connect(
        timeout: Duration,
        communicatorInitializer: FSRemoteCommsInitializer
    ): Result<Unit, RemoteCommsInitErr> = if (!isConnected) {
        if (this.communicatorInitializer !== communicatorInitializer) {
            this.communicatorInitializer = communicatorInitializer
        }
        when (val commsInitResult = communicatorInitializer.init(this, timeout)) {
            is Result.OK -> {
                communicator = commsInitResult.value
                Result.OK(Unit)
            }
            is Result.Failure -> commsInitResult
        }
    } else {
        Result.OK(Unit)
    }

    /**
     * Connects to device using the most recently used communicatorInitializer.
     * If currently connected this function will have no affect.
     */
    override suspend fun reconnect(timeout: Duration): Result<Unit, ReconnectError> = if (!isConnected) {
        val communicatorInitializer = this.communicatorInitializer
        if (communicatorInitializer == null) {
            Result.Failure(ReconnectError.NeverConnected)
        } else {
            when (val commsInitResult = communicatorInitializer.init(this, timeout)) {
                is Result.OK -> {
                    communicator = commsInitResult.value
                    Result.OK(Unit)
                }
                is Result.Failure -> Result.Failure(ReconnectError.InitError(commsInitResult.error))
            }
        }
    } else {
        Result.OK(Unit)
    }

    public final override suspend fun disconnect() {
        communicator?.close()
    }

    internal fun onCommunicatorClosed() {
        communicator = null
    }

}
