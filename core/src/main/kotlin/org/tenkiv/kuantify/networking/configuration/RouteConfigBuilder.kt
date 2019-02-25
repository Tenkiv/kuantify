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

package org.tenkiv.kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.networking.device.*

typealias Ping = Unit?
typealias PingReceiver = suspend () -> Unit
typealias MessageReceiver = suspend (update: String) -> Unit

private val logger = KotlinLogging.logger {}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Combined ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

@DslMarker
annotation class CombinedRouteMarker

internal class CombinedRouteConfig(private val device: FSDevice) {

    val networkRouteBindingMap = HashMap<Path, NetworkRouteBinding<*>>()

    val baseRoute: CombinedNetworkRouting get() = CombinedNetworkRouting(this, emptyList())

    fun <T> addRouteBinding(
        path: Path,
        isFullyBiDirectional: Boolean,
        build: CombinedRouteBindingBuilder<T>.() -> Unit
    ) {
        val routeHandlerBuilder = CombinedRouteBindingBuilder<T>()
        routeHandlerBuilder.build()

        val networkUpdateChannel = if ((device is LocalDevice && routeHandlerBuilder.receiveFromNetworkOnHost) ||
            (device is FSRemoteDevice && routeHandlerBuilder.receiveFromNetworkOnRemote)
        ) {
            //TODO: Might be able to change this to just be a function.
            Channel<String?>(10_000)
        } else {
            null
        }

        val receiveUpdatesOnHost: UpdateReceiver? = buildHostUpdateReceiver(routeHandlerBuilder)

        val receiveUpdatesOnRemote: UpdateReceiver? = buildRemoteUpdateReceiver(routeHandlerBuilder)

        val currentHandler = networkRouteBindingMap[path]
        if (currentHandler != null) {
            logger.warn { "Overriding combined route binding for route $path." }
        }

        networkRouteBindingMap[path] = when (device) {
            is LocalDevice -> NetworkRouteBinding.Host(
                device,
                path,
                routeHandlerBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.sendFromHost,
                receiveUpdatesOnHost
            )
            is FSRemoteDevice -> NetworkRouteBinding.Remote(
                device,
                path,
                routeHandlerBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.sendFromRemote,
                receiveUpdatesOnRemote,
                isFullyBiDirectional
            )
            else -> throw IllegalStateException(
                "Concrete FSDevice must extend either LocalDevice or FSRemoteDevice"
            )
        }
    }

    private fun <T> buildHostUpdateReceiver(routeBindingBuilder: CombinedRouteBindingBuilder<T>): UpdateReceiver? {
        if (routeBindingBuilder.receivePingOnEither == null &&
            routeBindingBuilder.receivePingOnHost == null &&
            routeBindingBuilder.withSerializer?.receiveMessageOnEither == null &&
            routeBindingBuilder.withSerializer?.receiveMessageOnHost == null &&
            routeBindingBuilder.onHost?.receivePing == null
        ) {
            return null
        } else {
            return { update ->
                if (update != null) {
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

    private fun <T> buildRemoteUpdateReceiver(routeBindingBuilder: CombinedRouteBindingBuilder<T>): UpdateReceiver? {
        if (routeBindingBuilder.receivePingOnEither == null &&
            routeBindingBuilder.receivePingOnRemote == null &&
            routeBindingBuilder.withSerializer?.receiveMessageOnEither == null &&
            routeBindingBuilder.withSerializer?.receiveMessageOnRemote == null
        ) {
            return null
        } else {
            return { update ->
                if (update != null) {
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
class CombinedNetworkRouting internal constructor(private val config: CombinedRouteConfig, private val path: Path) {

    fun <T> bind(
        vararg path: String,
        isFullyBiDirectional: Boolean,
        build: CombinedRouteBindingBuilder<T>.() -> Unit
    ) {
        bind(path.toList(), isFullyBiDirectional, build)
    }

    fun <T> bind(
        path: Path,
        isFullyBiDirectional: Boolean,
        build: CombinedRouteBindingBuilder<T>.() -> Unit
    ) {
        val path = this.path + path

        config.addRouteBinding(
            path,
            isFullyBiDirectional,
            build
        )
    }

    fun route(vararg path: String, build: CombinedNetworkRouting.() -> Unit) {
        route(path.toList(), build)
    }

    fun route(path: Path, build: CombinedNetworkRouting.() -> Unit) {
        val path = this.path + path

        CombinedNetworkRouting(config, path).apply(build)
    }
}

@CombinedRouteMarker
class CombinedRouteBindingBuilder<T> internal constructor() {
    internal var serializeMessage: MessageSerializer<T>? = null

    internal var withSerializer: WithSerializer? = null

    internal var onRemote: OnSide<T>? = null

    internal var onHost: OnSide<T>? = null

    internal var sendFromRemote: Boolean = false

    internal var sendFromHost: Boolean = false

    internal var receivePingOnEither: PingReceiver? = null

    internal var receivePingOnRemote: PingReceiver? = null

    internal var receivePingOnHost: PingReceiver? = null

    internal var localUpdateChannel: ReceiveChannel<T>? = null
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

    fun serializeMessage(messageSerializer: MessageSerializer<T>): SetSerializer<T> {
        serializeMessage = messageSerializer
        return SetSerializer(messageSerializer)
    }

    fun setLocalUpdateChannel(channel: ReceiveChannel<T>): SetUpdateChannel {
        localUpdateChannel = channel
        return SetUpdateChannel()
    }

    infix fun SetUpdateChannel.withUpdateChannel(build: WithUpdateChannel.() -> Unit) {
        val wuc = WithUpdateChannel()
        wuc.build()
        sendFromHost = wuc.sendFromHost
        sendFromRemote = wuc.sendFromRemote
    }

    fun receivePingOnEither(pingReceiver: PingReceiver) {
        receivePingOnEither = pingReceiver
    }

    fun receivePingOnRemote(pingReceiver: PingReceiver) {
        receivePingOnRemote = pingReceiver
    }

    fun receivePingOnHost(pingReceiver: PingReceiver) {
        receivePingOnHost = pingReceiver
    }

    infix fun SetSerializer<T>.withSerializer(build: WithSerializer.() -> Unit) {
        val ws = WithSerializer()
        ws.build()
        this@CombinedRouteBindingBuilder.withSerializer = ws
    }

    fun onRemote(build: OnSide<T>.() -> Unit) {
        val onSideBuilder = OnSide<T>()
        onSideBuilder.build()
        onRemote = onSideBuilder
        localUpdateChannel = onSideBuilder.localUpdateChannel
    }

    fun onHost(build: OnSide<T>.() -> Unit) {
        val onSideBuilder = OnSide<T>()
        onSideBuilder.build()
        onHost = onSideBuilder
        localUpdateChannel = onSideBuilder.localUpdateChannel
    }

    @CombinedRouteMarker
    class WithUpdateChannel {
        internal var sendFromHost: Boolean = false

        internal var sendFromRemote: Boolean = false

        fun sendFromHost() {
            sendFromHost = true
        }

        fun sendFromRemote() {
            sendFromRemote = true
        }
    }

}

@CombinedRouteMarker
class WithSerializer internal constructor() {

    internal var onRemote: OnSide? = null

    internal var onHost: OnSide? = null

    internal var receiveMessageOnEither: MessageReceiver? = null

    internal var receiveMessageOnRemote: MessageReceiver? = null

    internal var receiveMessageOnHost: MessageReceiver? = null

    fun receiveMessageOnEither(messageReceiver: MessageReceiver) {
        receiveMessageOnEither = messageReceiver
    }

    fun receiveMessageOnRemote(messageReceiver: MessageReceiver) {
        receiveMessageOnRemote = messageReceiver
    }

    fun receiveMessageOnHost(messageReceiver: MessageReceiver) {
        receiveMessageOnHost = messageReceiver
    }

    fun onRemote(build: OnSide.() -> Unit) {
        val onSideBuilder = OnSide()
        onSideBuilder.build()
        onRemote = onSideBuilder
    }

    fun onHost(build: OnSide.() -> Unit) {
        val onSideBuilder = OnSide()
        onSideBuilder.build()
        onHost = onSideBuilder
    }

    @CombinedRouteMarker
    class OnSide internal constructor() {

        internal var receiveMessage: MessageReceiver? = null

        fun receiveMessag(messageReceiver: MessageReceiver) {
            receiveMessage = messageReceiver
        }

    }

}

@CombinedRouteMarker
class SetSerializer<T> internal constructor(val serializer: MessageSerializer<T>)

@CombinedRouteMarker
class OnSide<T> internal constructor() {

    internal var localUpdateChannel: ReceiveChannel<T>? = null

    internal var send: Boolean = false

    internal var receivePing: PingReceiver? = null

    fun receivePing(pingReceiver: PingReceiver) {
        receivePing = pingReceiver
    }

    fun setLocalUpdateChannel(channel: ReceiveChannel<T>): SetUpdateChannel {
        localUpdateChannel = channel
        return SetUpdateChannel()
    }

    infix fun SetUpdateChannel.withUpdateChannel(build: SideWithUpdateChannel.() -> Unit) {
        val wuc = SideWithUpdateChannel()
        wuc.build()
        this@OnSide.send = wuc.send
    }

}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Separate ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

@DslMarker
annotation class SideRouteMarker

internal class SideRouteConfig(private val device: FSBaseDevice) {

    val networkRouteBindingMap = HashMap<Path, NetworkRouteBinding<*>>()

    val baseRoute: SideNetworkRouting get() = SideNetworkRouting(this, emptyList())

    fun <T> addRouteBinding(
        path: Path,
        isFullyBiDirectional: Boolean,
        build: SideRouteBindingBuilder<T>.() -> Unit
    ) {
        val routeHandlerBuilder = SideRouteBindingBuilder<T>()
        routeHandlerBuilder.build()

        val networkUpdateChannel = if (routeHandlerBuilder.receive != null) {
            Channel<String?>(10_000)
        } else {
            null
        }

        val currentHandler = networkRouteBindingMap[path]
        if (currentHandler != null) {
            logger.warn { "Overriding side route binding for route $path." }
        }

        networkRouteBindingMap[path] = when (device) {
            is LocalDevice -> NetworkRouteBinding.Host(
                device,
                path,
                routeHandlerBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.send,
                routeHandlerBuilder.receive
            )
            is FSRemoteDevice -> NetworkRouteBinding.Remote(
                device,
                path,
                routeHandlerBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.send,
                routeHandlerBuilder.receive,
                isFullyBiDirectional
            )
        }
    }
}

@Suppress("NAME_SHADOWING")
@SideRouteMarker
class SideNetworkRouting internal constructor(private val config: SideRouteConfig, private val path: Path) {

    fun <T> bind(
        vararg path: String,
        isFullyBiDirectional: Boolean,
        build: SideRouteBindingBuilder<T>.() -> Unit
    ) {
        bind(path.toList(), isFullyBiDirectional, build)
    }

    fun <T> bind(
        path: Path,
        isFullyBiDirectional: Boolean,
        build: SideRouteBindingBuilder<T>.() -> Unit
    ) {
        val path = this.path + path

        config.addRouteBinding(
            path,
            isFullyBiDirectional = isFullyBiDirectional,
            build = build
        )
    }

    fun route(vararg path: String, build: SideNetworkRouting.() -> Unit) {
        route(path.toList(), build)
    }


    fun route(path: Path, build: SideNetworkRouting.() -> Unit) {
        val path = this.path + path

        SideNetworkRouting(config, path).apply(build)
    }
}

@SideRouteMarker
class SideRouteBindingBuilder<T> internal constructor() {

    internal var localUpdateChannel: ReceiveChannel<T>? = null

    internal var serializeMessage: MessageSerializer<T>? = null

    internal var send: Boolean = false

    @PublishedApi
    internal var receive: UpdateReceiver? = null

    fun serializeMessage(messageSerializer: MessageSerializer<T>) {
        serializeMessage = messageSerializer
    }

    fun setLocalUpdateChannel(channel: ReceiveChannel<T>): SetUpdateChannel {
        localUpdateChannel = channel
        return SetUpdateChannel()
    }

    fun receive(receiver: UpdateReceiver) {
        receive = receiver
    }

    inline fun receiveMessage(resolutionStrategy: NullResolutionStrategy, crossinline receiver: MessageReceiver) {
        receive = { message ->
            if (message == null) {
                if (resolutionStrategy === NullResolutionStrategy.PANIC) {
                    TODO("throw specific exception")
                }
            } else {
                receiver(message)
            }
        }
    }

    infix fun SetUpdateChannel.withUpdateChannel(build: SideWithUpdateChannel.() -> Unit) {
        val wuc = SideWithUpdateChannel()
        wuc.build()
        this@SideRouteBindingBuilder.send = wuc.send
    }
}

@SideRouteMarker
class SetUpdateChannel internal constructor()

enum class NullResolutionStrategy {
    PANIC, SKIP
}

@CombinedRouteMarker
@SideRouteMarker
class SideWithUpdateChannel {
    internal var send: Boolean = false

    fun send() {
        send = true
    }
}