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

package kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kuantify.*
import kuantify.networking.communication.*
import mu.*
import org.tenkiv.coral.*

@PublishedApi
internal val routeConfigBuilderLogger: KLogger = KotlinLogging.logger {}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Main route config container class ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
@KuantifyComponentBuilder
public class RouteConfig<SerialT : Any>(
    @PublishedApi internal val communicator: Communicator<SerialT>,
    @PublishedApi internal val serializedPing: SerialT,
    @PublishedApi internal val formatPath: (Path) -> String
) {
    public val networkRouteBindingMap: HashMap<String, NetworkRouteBinding<SerialT>> = HashMap()

    public val baseRoute: NetworkRoute<SerialT>
        get() = NetworkRoute(this, emptyList())

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

        networkRouteBindingMap[path] = MessageBinding(
            communicator,
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
            PingReceiver(Channel(capacity = Channel.RENDEZVOUS), receiveOp)
        } else {
            null
        }

        networkRouteBindingMap[path] = PingBinding(
            communicator,
            path,
            routeBindingBuilder.localUpdateChannel,
            networkPingReceiver,
            serializedPing
        )
    }

    public companion object {

        public fun formatPathStandard(path: Path): String {
            var result = ""
            path.forEachIndexed { index, value ->
                val append = if (index != path.lastIndex) "/" else ""
                result += "$value$append"
            }
            return result
        }

    }

}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Network route builder - plain route and bind ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
@Suppress("NAME_SHADOWING")
@NetworkingDsl
public class NetworkRoute<SerialT : Any> @PublishedApi internal constructor(
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

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Plain binding builders ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
@NetworkingDsl
public class MessageBindingBuilder<BoundT, SerialT : Any> @PublishedApi internal constructor() {
    @PublishedApi
    internal var send: MessageSender<BoundT, SerialT>? = null

    @PublishedApi
    internal var receive: MessageReceiver<SerialT>? = null

    @NetworkingDsl
    public fun send(source: Flow<BoundT>, serialize: MessageSerializer<BoundT, SerialT>) {
        send = MessageSender.Flow(source, serialize)
    }

    public fun send(source: ReceiveChannel<BoundT>, serialize: MessageSerializer<BoundT, SerialT>) {
        send = MessageSender.Channel(source, serialize)
    }

    /**
     * Receive messages through a dedicated [Channel] on a dedicated coroutine.
     */
    @NetworkingDsl
    public fun receive(
        networkChannelCapacity: Int32 = Channel.BUFFERED,
        receiveOp: ReceiveMessage<SerialT>
    ) {
        receive = MessageReceiver.Dedicated(Channel(networkChannelCapacity), receiveOp)
    }

    /**
     * **Warning** -Improper use of this receive method can easily break communication. Default to using regular
     * [receive] unless you're sure you know what you're doing.
     *
     * Receive messages directly in the coroutine(s) pulling messages in from the communication source. This method
     * should only be used when there is virtually nothing done in the [receiveOp] as any time taken up in the
     * [receiveOp] will block reception of all messages on all routes.
     *
     * [receiveOp] should only suspend as a way of handling backpressure if a buffer is full.
     */
    @NetworkingDsl
    public fun receiveDirect(receiveOp: ReceiveMessage<SerialT>) {
        receive = MessageReceiver.Direct(receiveOp)
    }

}

@NetworkingDsl
public class PingBindingBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal var localUpdateChannel: ReceiveChannel<Ping>? = null

    @PublishedApi
    internal var receiveOp: ReceivePing? = null

    @NetworkingDsl
    public fun send(source: ReceiveChannel<Ping>) {
        localUpdateChannel = source
    }

    @NetworkingDsl
    public fun receive(
        receiveOp: ReceivePing
    ) {
        this.receiveOp = receiveOp
    }
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ String format kotlinx.serialization bindings ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
@NetworkingDsl
public class StringSerializingMbb<BoundT> @PublishedApi internal constructor(
    @PublishedApi internal val formatter: StringFormat,
    @PublishedApi internal val serializer: KSerializer<BoundT>
) {
    @PublishedApi
    internal val parent: MessageBindingBuilder<BoundT, String> = MessageBindingBuilder()

    @NetworkingDsl
    public fun send(source: Flow<BoundT>) {
        parent.send = MessageSender.Flow(source) { value ->
            formatter.encodeToString(serializer, value)
        }
    }

    @NetworkingDsl
    public fun send(source: ReceiveChannel<BoundT>) {
        parent.send = MessageSender.Channel(source) { value ->
            formatter.encodeToString(serializer, value)
        }
    }

    /**
     * Receive messages through a dedicated [Channel] on a dedicated coroutine.
     */
    @NetworkingDsl
    public inline fun receive(
        networkChannelCapacity: Int32 = Channel.BUFFERED,
        crossinline receiveOp: suspend (BoundT) -> Unit
    ) {
        parent.receive = MessageReceiver.Dedicated(Channel(networkChannelCapacity)) { value ->
            receiveOp(formatter.decodeFromString(serializer, value))
        }
    }

    /**
     * **Warning** -Improper use of this receive method can easily break communication. Default to using regular
     * [receive] unless you're sure you know what you're doing.
     *
     * Receive messages directly in the coroutine(s) pulling messages in from the communication source. This method
     * should only be used when there is virtually nothing done in the [receiveOp] as any time taken up in the
     * [receiveOp] will block reception of all messages on all routes.
     *
     * [receiveOp] should only suspend as a way of handling backpressure if a buffer is full.
     */
    @NetworkingDsl
    public inline fun receiveDirect(crossinline receiveOp: suspend (BoundT) -> Unit) {
        parent.receive = MessageReceiver.Direct { value ->
            receiveOp(formatter.decodeFromString(serializer, value))
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
public inline fun <reified BoundT> NetworkRoute<String>.bind(
    formatter: StringFormat,
    path: Path,
    build: StringSerializingMbb<BoundT>.() -> Unit
) {
    bind(formatter, formatter.serializersModule.serializer(), path, build)
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
public inline fun <reified BoundT> NetworkRoute<String>.bind(
    formatter: StringFormat,
    vararg path: String,
    build: StringSerializingMbb<BoundT>.() -> Unit
) {
    bind(formatter, formatter.serializersModule.serializer(), path.toList(), build)
}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Binary format kotlinx.serialization bindings ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
@NetworkingDsl
public class BinarySerializingMbb<BoundT> @PublishedApi internal constructor(
    @PublishedApi internal val formatter: BinaryFormat,
    @PublishedApi internal val serializer: KSerializer<BoundT>
) {
    @PublishedApi
    internal val parent: MessageBindingBuilder<BoundT, ByteArray> = MessageBindingBuilder()

    @NetworkingDsl
    public fun send(source: Flow<BoundT>) {
        parent.send = MessageSender.Flow(source) { value ->
            formatter.encodeToByteArray(serializer, value)
        }
    }

    @NetworkingDsl
    public fun send(source: ReceiveChannel<BoundT>) {
        parent.send = MessageSender.Channel(source) { value ->
            formatter.encodeToByteArray(serializer, value)
        }
    }

    /**
     * Receive messages through a dedicated [Channel] on a dedicated coroutine.
     */
    @NetworkingDsl
    public inline fun receive(
        networkChannelCapacity: Int32 = Channel.BUFFERED,
        crossinline receiveOp: suspend (BoundT) -> Unit
    ) {
        parent.receive = MessageReceiver.Dedicated(Channel(networkChannelCapacity)) { value ->
            receiveOp(formatter.decodeFromByteArray(serializer, value))
        }
    }

    /**
     * **Warning** -Improper use of this receive method can easily break communication. Default to using regular
     * [receive] unless you're sure you know what you're doing.
     *
     * Receive messages directly in the coroutine(s) pulling messages in from the communication source. This method
     * should only be used when there is virtually nothing done in the [receiveOp] as any time taken up in the
     * [receiveOp] will block reception of all messages on all routes.
     *
     * [receiveOp] should only suspend as a way of handling backpressure if a buffer is full.
     */
    @NetworkingDsl
    public inline fun receiveDirect(crossinline receiveOp: suspend (BoundT) -> Unit) {
        parent.receive = MessageReceiver.Direct { value ->
            receiveOp(formatter.decodeFromByteArray(serializer, value))
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
public inline fun <reified BoundT> NetworkRoute<ByteArray>.bind(
    formatter: BinaryFormat,
    path: Path,
    build: BinarySerializingMbb<BoundT>.() -> Unit
) {
    bind(formatter, formatter.serializersModule.serializer(), path, build)
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
public inline fun <reified BoundT> NetworkRoute<ByteArray>.bind(
    formatter: BinaryFormat,
    vararg path: String,
    build: BinarySerializingMbb<BoundT>.() -> Unit
) {
    bind(formatter, formatter.serializersModule.serializer(), path.toList(), build)
}