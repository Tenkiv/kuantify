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

package org.tenkiv.kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import mu.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.networking.communication.*

@PublishedApi
internal val routeConfigBuilderLogger = KotlinLogging.logger {}

@KuantifyComponentBuilder
public class RouteConfig<SerialT>(
    @PublishedApi internal val networkCommunicator: NetworkCommunicator<SerialT>,
    @PublishedApi internal val serializedPing: SerialT,
    @PublishedApi internal val formatPath: (Path) -> String
) {
    public val networkRouteBindingMap: HashMap<String, NetworkRouteBinding<SerialT>> = HashMap()

    public val baseRoute: NetworkRoute<SerialT>
        get() = NetworkRoute(this, emptyList())

    @PublishedApi
    internal val remoteConnectionCommunicator: Boolean =
        (networkCommunicator as? RemoteNetworkCommunicator)?.communicationMode != CommunicationMode.NO_CONNECTION

    @Suppress("NAME_SHADOWING")
    @PublishedApi
    internal fun <BoundT> addMessageBinding(
        path: Path,
        routeBindingBuilder: MessageBindingBuilder<BoundT, SerialT>
    ) {
        val path = formatPath(path)

        val currentBinding = networkRouteBindingMap[path]
        if (currentBinding != null) {
            //TODO: Consider throwing exception here instead of just warning.
            routeConfigBuilderLogger.warn { "Overriding side route binding for route $path." }
        }

        networkRouteBindingMap[path] = NetworkMessageBinding(
            networkCommunicator,
            path,
            routeBindingBuilder.send,
            routeBindingBuilder.receive
        )
    }

    @Suppress("NAME_SHADOWING")
    @PublishedApi
    internal fun addPingBinding(
        path: Path,
        routeBindingBuilder: PingBindingBuilder
    ) {
        val path = formatPath(path)

        val currentBinding = networkRouteBindingMap[path]
        if (currentBinding != null) {
            routeConfigBuilderLogger.warn { "Overriding side route binding for route $path." }
        }

        val receiveOp = routeBindingBuilder.receiveOp
        val networkPingReceiver = if (receiveOp != null) {
            NetworkPingReceiver(Channel(capacity = Channel.RENDEZVOUS), receiveOp)
        } else {
            null
        }

        networkRouteBindingMap[path] = NetworkPingBinding(
            networkCommunicator,
            path,
            routeBindingBuilder.localUpdateChannel,
            networkPingReceiver,
            serializedPing
        )
    }

}

@Suppress("NAME_SHADOWING")
@NetworkingDsl
public class NetworkRoute<SerialT> @PublishedApi internal constructor(
    @PublishedApi internal val config: RouteConfig<SerialT>,
    @PublishedApi internal val path: Path
) {

    @NetworkingDsl
    public inline fun <BoundT> bind(
        vararg path: String,
        build: MessageBindingBuilder<BoundT, SerialT>.() -> Unit
    ) {
        bind(path.toList(), build)
    }

    @NetworkingDsl
    public inline fun bindPing(
        vararg path: String,
        build: PingBindingBuilder.() -> Unit
    ) {
        bindPing(path.toList(), build)
    }

    @NetworkingDsl
    public inline fun <BoundT> bind(
        path: Path,
        build: MessageBindingBuilder<BoundT, SerialT>.() -> Unit
    ) {
        val path = this.path + path
        val builder = MessageBindingBuilder<BoundT, SerialT>().apply(build)

        config.addMessageBinding(path, builder)
    }

    @NetworkingDsl
    public inline fun bindPing(
        path: Path,
        build: PingBindingBuilder.() -> Unit
    ) {
        val path = this.path + path
        val builder = PingBindingBuilder().apply(build)

        config.addPingBinding(path, builder)
    }

    @NetworkingDsl
    public inline fun route(vararg path: String, build: NetworkRoute<SerialT>.() -> Unit) {
        route(path.toList(), build)
    }

    @NetworkingDsl
    public inline fun route(path: Path, build: NetworkRoute<SerialT>.() -> Unit) {
        val path = this.path + path

        NetworkRoute(config, path).apply(build)
    }
}

@NetworkingDsl
public class MessageBindingBuilder<BoundT, SerialT> @PublishedApi internal constructor() {
    @PublishedApi
    internal var send: LocalUpdateSender<BoundT, SerialT>? = null

    @PublishedApi
    internal var receive: NetworkMessageReceiver<SerialT>? = null

    @NetworkingDsl
    public fun send(source: ReceiveChannel<BoundT>, serialize: MessageSerializer<BoundT, SerialT>) {
        send = LocalUpdateSender(source, serialize)
    }

    @NetworkingDsl
    public fun receive(
        networkChannelCapacity: Int = Channel.BUFFERED,
        receiveOp: MessageReceiver<SerialT>
    ) {
        receive = NetworkMessageReceiver(Channel(networkChannelCapacity), receiveOp)
    }

}

@NetworkingDsl
public class StringSerializingMbb<BoundT> @PublishedApi internal constructor(
    @PublishedApi internal val formatter: StringFormat,
    @PublishedApi internal val serializer: KSerializer<BoundT>
) {
    @PublishedApi
    internal val parent = MessageBindingBuilder<BoundT, String>()

    @NetworkingDsl
    public fun send(source: ReceiveChannel<BoundT>) {
        parent.send = LocalUpdateSender(source) { value ->
            formatter.stringify(serializer, value)
        }
    }

    @NetworkingDsl
    public inline fun receive(
        networkChannelCapacity: Int = Channel.BUFFERED,
        crossinline receiveOp: suspend (BoundT) -> Unit
    ) {
        parent.receive = NetworkMessageReceiver(Channel(networkChannelCapacity)) { value ->
            receiveOp(formatter.parse(serializer, value))
        }
    }

}

@Suppress("NAME_SHADOWING")
@NetworkingDsl
public inline fun <BoundT> NetworkRoute<String>.bind(
    formatter: StringFormat,
    serializer: KSerializer<BoundT>,
    path: Path,
    build: StringSerializingMbb<BoundT>.() -> Unit
) {
    val path = this.path + path
    val builder = StringSerializingMbb(formatter, serializer).apply(build).parent

    config.addMessageBinding(path, builder)
}

@NetworkingDsl
public inline fun <BoundT> NetworkRoute<String>.bind(
    formatter: StringFormat,
    serializer: KSerializer<BoundT>,
    vararg path: String,
    build: StringSerializingMbb<BoundT>.() -> Unit
) {
    bind(formatter, serializer, path.toList(), build)
}

@NetworkingDsl
public class BinarySerializingMbb<BoundT> @PublishedApi internal constructor(
    @PublishedApi internal val formatter: BinaryFormat,
    @PublishedApi internal val serializer: KSerializer<BoundT>
) {
    @PublishedApi
    internal val parent = MessageBindingBuilder<BoundT, ByteArray>()

    @NetworkingDsl
    public fun send(source: ReceiveChannel<BoundT>) {
        parent.send = LocalUpdateSender(source) { value ->
            formatter.dump(serializer, value)
        }
    }

    @NetworkingDsl
    public inline fun receive(
        networkChannelCapacity: Int = Channel.BUFFERED,
        crossinline receiveOp: suspend (BoundT) -> Unit
    ) {
        parent.receive = NetworkMessageReceiver(Channel(networkChannelCapacity)) { value ->
            receiveOp(formatter.load(serializer, value))
        }
    }

}

@Suppress("NAME_SHADOWING")
@NetworkingDsl
public inline fun <BoundT> NetworkRoute<ByteArray>.bind(
    formatter: BinaryFormat,
    serializer: KSerializer<BoundT>,
    path: Path,
    build: BinarySerializingMbb<BoundT>.() -> Unit
) {
    val path = this.path + path
    val builder = BinarySerializingMbb(formatter, serializer).apply(build).parent

    config.addMessageBinding(path, builder)
}

@NetworkingDsl
public inline fun <BoundT> NetworkRoute<ByteArray>.bind(
    formatter: BinaryFormat,
    serializer: KSerializer<BoundT>,
    vararg path: String,
    build: BinarySerializingMbb<BoundT>.() -> Unit
) {
    bind(formatter, serializer, path.toList(), build)
}

@NetworkingDsl
public class PingBindingBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal var localUpdateChannel: ReceiveChannel<Ping>? = null

    @PublishedApi
    internal var receiveOp: PingReceiver? = null

    @NetworkingDsl
    public fun send(source: ReceiveChannel<Ping>) {
        localUpdateChannel = source
    }

    @NetworkingDsl
    public fun receive(
        receiveOp: PingReceiver
    ) {
        this.receiveOp = receiveOp
    }
}