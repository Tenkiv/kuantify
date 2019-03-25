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

package org.tenkiv.kuantify.fs.networking.configuration

import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.networking.communication.*
import org.tenkiv.kuantify.networking.configuration.*

public typealias PingReceiver = suspend () -> Unit

private typealias FSMessageReceiver = UpdateReceiver<String>
private typealias FSMessageSerializer<MT> = MessageSerializer<MT, String>

private val logger = KotlinLogging.logger {}

public fun formatPathStandard(path: Path): String {
    var result = ""
    path.forEachIndexed { index, value ->
        val append = if (index != path.lastIndex) "/" else ""
        result += "$value$append"
    }
    return result
}

@DslMarker
internal annotation class CombinedRouteMarker

public class CombinedRouteConfig(private val networkCommunicator: NetworkCommunicator<String>) {

    val networkRouteBindingMap = HashMap<String, NetworkRouteBinding<*, String>>()

    val baseRoute: CombinedNetworkRouting get() = CombinedNetworkRouting(this, emptyList())

    private val device get() = networkCommunicator.device

    private val remoteConnectionCommunicator: Boolean =
        (networkCommunicator as? RemoteNetworkCommunicator)?.communicationMode != CommunicationMode.NO_CONNECTION

    @Suppress("NAME_SHADOWING")
    fun <T> addRouteBinding(
        path: Path,
        recursiveSynchronizer: Boolean,
        build: CombinedRouteBindingBuilder<T>.() -> Unit
    ) {
        val path = formatPathStandard(path)
        val routeBindingBuilder = CombinedRouteBindingBuilder<T>()
        routeBindingBuilder.build()

        val networkUpdateChannel = if ((device is LocalDevice && routeBindingBuilder.receiveFromNetworkOnHost) ||
            (device is FSRemoteDevice && routeBindingBuilder.receiveFromNetworkOnRemote)
        ) {
            Channel<String>(Channel.UNLIMITED)
        } else {
            null
        }

        val currentBinding = networkRouteBindingMap[path]
        if (currentBinding != null) {
            logger.warn { "Overriding combined route binding for route $path." }
        }

        fun standardRouteBinding() = StandardRouteBinding(
            networkCommunicator,
            path,
            routeBindingBuilder.localUpdateChannel,
            networkUpdateChannel,
            routeBindingBuilder.serializeMessage,
            routeBindingBuilder.sendFromHost,
            buildHostUpdateReceiver(routeBindingBuilder),
            FSDevice.serializedPing
        )

        fun recursionPreventingRouteBinding() = RecursionPreventingRouteBinding(
            networkCommunicator,
            path,
            routeBindingBuilder.localUpdateChannel,
            networkUpdateChannel,
            routeBindingBuilder.serializeMessage,
            routeBindingBuilder.sendFromRemote,
            buildRemoteUpdateReceiver(routeBindingBuilder),
            FSDevice.serializedPing
        )

        networkRouteBindingMap[path] = if (remoteConnectionCommunicator && recursiveSynchronizer) {
            recursionPreventingRouteBinding()
        } else {
            standardRouteBinding()
        }

    }

    private fun <T> buildHostUpdateReceiver(routeBindingBuilder: CombinedRouteBindingBuilder<T>): FSMessageReceiver? {
        if (routeBindingBuilder.receivePingOnEither == null &&
            routeBindingBuilder.receivePingOnHost == null &&
            routeBindingBuilder.withSerializer?.receiveMessageOnEither == null &&
            routeBindingBuilder.withSerializer?.receiveMessageOnHost == null &&
            routeBindingBuilder.onHost?.receivePing == null
        ) {
            return null
        } else {
            return { update ->
                if (update != FSDevice.serializedPing) {
                    routeBindingBuilder.withSerializer?.receiveMessageOnEither?.invoke(update)
                    routeBindingBuilder.withSerializer?.receiveMessageOnHost?.invoke(update)
                } else {
                    routeBindingBuilder.receivePingOnEither?.invoke()
                    routeBindingBuilder.receivePingOnHost?.invoke()
                    routeBindingBuilder.onHost?.receivePing?.invoke()
                }
            }
        }
    }

    private fun <T> buildRemoteUpdateReceiver(routeBindingBuilder: CombinedRouteBindingBuilder<T>): FSMessageReceiver? {
        if (routeBindingBuilder.receivePingOnEither == null &&
            routeBindingBuilder.receivePingOnRemote == null &&
            routeBindingBuilder.withSerializer?.receiveMessageOnEither == null &&
            routeBindingBuilder.withSerializer?.receiveMessageOnRemote == null
        ) {
            return null
        } else {
            return { update ->
                if (update != FSDevice.serializedPing) {
                    routeBindingBuilder.withSerializer?.receiveMessageOnEither?.invoke(update)
                    routeBindingBuilder.withSerializer?.receiveMessageOnRemote?.invoke(update)
                } else {
                    routeBindingBuilder.receivePingOnEither?.invoke()
                    routeBindingBuilder.receivePingOnRemote?.invoke()
                }
            }
        }
    }
}

@Suppress("NAME_SHADOWING")
@CombinedRouteMarker
public class CombinedNetworkRouting internal constructor(
    private val config: CombinedRouteConfig,
    private val path: Path
) {

    public fun <T> bind(
        vararg path: String,
        recursiveSynchronizer: Boolean = false,
        build: CombinedRouteBindingBuilder<T>.() -> Unit
    ) {
        bind(path.toList(), recursiveSynchronizer, build)
    }

    public fun <T> bind(
        path: Path,
        recursiveSynchronizer: Boolean = false,
        build: CombinedRouteBindingBuilder<T>.() -> Unit
    ) {
        val path = this.path + path

        config.addRouteBinding(
            path,
            recursiveSynchronizer,
            build
        )
    }

    public fun route(vararg path: String, build: CombinedNetworkRouting.() -> Unit) {
        route(path.toList(), build)
    }

    public fun route(path: Path, build: CombinedNetworkRouting.() -> Unit) {
        val path = this.path + path

        CombinedNetworkRouting(config, path).apply(build)
    }
}

@CombinedRouteMarker
public class CombinedRouteBindingBuilder<MT> internal constructor() {
    internal var serializeMessage: FSMessageSerializer<MT>? = null

    internal var withSerializer: WithSerializer? = null

    internal var onRemote: OnSide<MT>? = null

    internal var onHost: OnSide<MT>? = null

    internal var sendFromRemote: Boolean = false

    internal var sendFromHost: Boolean = false

    internal var receivePingOnEither: PingReceiver? = null

    internal var receivePingOnRemote: PingReceiver? = null

    internal var receivePingOnHost: PingReceiver? = null

    internal var localUpdateChannel: ReceiveChannel<MT>? = null
        set(value) {
            if (field != null) {
                logger.warn { "localUpdateChannel for route binding was overriden" }
            }
            field = value
        }

    internal val receiveFromNetworkOnHost
        get() = receivePingOnEither != null ||
                receivePingOnHost != null ||
                onHost?.receivePing != null ||
                withSerializer?.receiveMessageOnEither != null ||
                withSerializer?.receiveMessageOnHost != null

    internal val receiveFromNetworkOnRemote
        get() = receivePingOnEither != null ||
                receivePingOnRemote != null ||
                onRemote?.receivePing != null ||
                withSerializer?.receiveMessageOnEither != null ||
                withSerializer?.receiveMessageOnRemote != null

    public fun serializeMessage(messageSerializer: FSMessageSerializer<MT>): SetSerializer<MT> {
        serializeMessage = messageSerializer
        return SetSerializer(messageSerializer)
    }

    public fun setLocalUpdateChannel(channel: ReceiveChannel<MT>): SetUpdateChannel {
        localUpdateChannel = channel
        return SetUpdateChannel()
    }

    public infix fun SetUpdateChannel.withUpdateChannel(build: WithUpdateChannel.() -> Unit) {
        val wuc = WithUpdateChannel()
        wuc.build()
        sendFromHost = wuc.sendFromHost
        sendFromRemote = wuc.sendFromRemote
    }

    public fun receivePingOnEither(pingReceiver: PingReceiver) {
        receivePingOnEither = pingReceiver
    }

    public fun receivePingOnRemote(pingReceiver: PingReceiver) {
        receivePingOnRemote = pingReceiver
    }

    public fun receivePingOnHost(pingReceiver: PingReceiver) {
        receivePingOnHost = pingReceiver
    }

    public infix fun SetSerializer<MT>.withSerializer(build: WithSerializer.() -> Unit) {
        val ws = WithSerializer()
        ws.build()
        this@CombinedRouteBindingBuilder.withSerializer = ws
    }

    public fun onRemote(build: OnSide<MT>.() -> Unit) {
        val onSideBuilder = OnSide<MT>()
        onSideBuilder.build()
        onRemote = onSideBuilder
        localUpdateChannel = onSideBuilder.localUpdateChannel
    }

    public fun onHost(build: OnSide<MT>.() -> Unit) {
        val onSideBuilder = OnSide<MT>()
        onSideBuilder.build()
        onHost = onSideBuilder
        localUpdateChannel = onSideBuilder.localUpdateChannel
    }

    @CombinedRouteMarker
    public class WithUpdateChannel {
        internal var sendFromHost: Boolean = false

        internal var sendFromRemote: Boolean = false

        public fun sendFromHost() {
            sendFromHost = true
        }

        public fun sendFromRemote() {
            sendFromRemote = true
        }
    }

}

@CombinedRouteMarker
public class WithSerializer internal constructor() {

    internal var onRemote: OnSide? = null

    internal var onHost: OnSide? = null

    internal var receiveMessageOnEither: FSMessageReceiver? = null

    internal var receiveMessageOnRemote: FSMessageReceiver? = null

    internal var receiveMessageOnHost: FSMessageReceiver? = null

    public fun receiveMessageOnEither(messageReceiver: FSMessageReceiver) {
        receiveMessageOnEither = messageReceiver
    }

    public fun receiveMessageOnRemote(messageReceiver: FSMessageReceiver) {
        receiveMessageOnRemote = messageReceiver
    }

    public fun receiveMessageOnHost(messageReceiver: FSMessageReceiver) {
        receiveMessageOnHost = messageReceiver
    }

    public fun onRemote(build: OnSide.() -> Unit) {
        val onSideBuilder = OnSide()
        onSideBuilder.build()
        onRemote = onSideBuilder
    }

    public fun onHost(build: OnSide.() -> Unit) {
        val onSideBuilder = OnSide()
        onSideBuilder.build()
        onHost = onSideBuilder
    }

    @CombinedRouteMarker
    public class OnSide internal constructor() {

        internal var receiveMessage: FSMessageReceiver? = null

        public fun receiveMessag(messageReceiver: FSMessageReceiver) {
            receiveMessage = messageReceiver
        }

    }

}

@CombinedRouteMarker
public class SetSerializer<MT> internal constructor(val serializer: FSMessageSerializer<MT>)

@CombinedRouteMarker
public class OnSide<MT> internal constructor() {

    internal var localUpdateChannel: ReceiveChannel<MT>? = null

    internal var send: Boolean = false

    internal var receivePing: PingReceiver? = null

    public fun receivePing(pingReceiver: PingReceiver) {
        receivePing = pingReceiver
    }

    public fun setLocalUpdateChannel(channel: ReceiveChannel<MT>): SetUpdateChannel {
        localUpdateChannel = channel
        return SetUpdateChannel()
    }

    public infix fun SetUpdateChannel.withUpdateChannel(build: SideWithUpdateChannel.() -> Unit) {
        val wuc = SideWithUpdateChannel()
        wuc.build()
        this@OnSide.send = wuc.send
    }

}