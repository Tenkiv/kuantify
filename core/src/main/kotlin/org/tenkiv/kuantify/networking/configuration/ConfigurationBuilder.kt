package org.tenkiv.kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.device.*

typealias Ping = Unit?
typealias PingReceiver = suspend () -> Unit
typealias MessageReceiver = suspend (update: String) -> Unit

private val logger = KotlinLogging.logger {}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Combined ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

@DslMarker
annotation class CombinedRouteConfigMarker

@CombinedRouteConfigMarker
class CombinedRouteConfig internal constructor(private val device: FSDevice) {

    internal val networkRouteHandlerMap = HashMap<Route, NetworkRouteHandler<*>>()

    internal val networkUpdateChannelMap = HashMap<Route, Channel<String?>>()

    fun add(additions: CombinedRouteConfig.() -> Unit) {
        this.additions()
    }

    fun route(vararg path: String): Route = listOf(*path)

    fun route(path: List<String>): Route = path

    fun <T> handler(
        isFullyBiDirectional: Boolean,
        build: CombinedRouteHandlerBuilder<T>.() -> Unit
    ): HandlerParams<T> {
        return HandlerParams(isFullyBiDirectional, build)
    }

    @Suppress("NAME_SHADOWING")
    infix fun <T> Route.to(handler: HandlerParams<T>) {
        val (isFullyBiDirectional, build) = handler

        val routeHandlerBuilder = CombinedRouteHandlerBuilder<T>()
        routeHandlerBuilder.build()

        val networkUpdateChannel = if ((device is LocalDevice && routeHandlerBuilder.receiveFromNetworkOnHost) ||
            (device is FSRemoteDevice && routeHandlerBuilder.receiveFromNetworkOnRemote)
        ) {
            Channel<String?>(10_000).also { networkUpdateChannelMap += this to it }
        } else {
            null
        }

        val receiveUpdatesOnHost: UpdateReceiver? = buildHostUpdateReceiver(routeHandlerBuilder)

        val receiveUpdatesOnRemote: UpdateReceiver? = buildRemoteUpdateReceiver(routeHandlerBuilder)

        val currentHandler = networkRouteHandlerMap[this]
        if (currentHandler != null) {
            logger.warn { "Overriding combined routing for route $this." }
        }

        networkRouteHandlerMap[this] = when (device) {
            is LocalDevice -> NetworkRouteHandler.Host(
                device,
                this,
                routeHandlerBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.sendFromHost,
                receiveUpdatesOnHost
            )
            is FSRemoteDevice -> NetworkRouteHandler.Remote(
                device,
                this,
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

    private fun <T> buildHostUpdateReceiver(routeHandlerBuilder: CombinedRouteHandlerBuilder<T>): UpdateReceiver? {
        if (routeHandlerBuilder.receivePingOnEither == null &&
            routeHandlerBuilder.receivePingOnHost == null &&
            routeHandlerBuilder.withSerializer?.receiveMessageOnEither == null &&
            routeHandlerBuilder.withSerializer?.receiveMessageOnHost == null &&
            routeHandlerBuilder.onHost?.receivePing == null
        ) {
            return null
        } else {
            return { update ->
                if (update != null) {
                    routeHandlerBuilder.withSerializer?.receiveMessageOnEither?.invoke(update)
                    routeHandlerBuilder.withSerializer?.receiveMessageOnHost?.invoke(update)
                } else {
                    routeHandlerBuilder.receivePingOnEither?.invoke()
                    routeHandlerBuilder.receivePingOnHost?.invoke()
                    routeHandlerBuilder.onHost?.receivePing?.invoke()
                }
            }
        }
    }

    private fun <T> buildRemoteUpdateReceiver(routeHandlerBuilder: CombinedRouteHandlerBuilder<T>): UpdateReceiver? {
        if (routeHandlerBuilder.receivePingOnEither == null &&
            routeHandlerBuilder.receivePingOnRemote == null &&
            routeHandlerBuilder.withSerializer?.receiveMessageOnEither == null &&
            routeHandlerBuilder.withSerializer?.receiveMessageOnRemote == null
        ) {
            return null
        } else {
            return { update ->
                if (update != null) {
                    routeHandlerBuilder.withSerializer?.receiveMessageOnEither?.invoke(update)
                    routeHandlerBuilder.withSerializer?.receiveMessageOnRemote?.invoke(update)
                } else {
                    routeHandlerBuilder.receivePingOnEither?.invoke()
                    routeHandlerBuilder.receivePingOnRemote?.invoke()
                }
            }
        }
    }

    data class HandlerParams<T> internal constructor(
        val isFullyBiDirectional: Boolean,
        val build: CombinedRouteHandlerBuilder<T>.() -> Unit
    )
}

@CombinedRouteConfigMarker
class CombinedRouteHandlerBuilder<T> internal constructor() {
    internal var serializeMessage: MessageSerializer<T>? = null

    internal var withSerializer: WithSerializer<T>? = null

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
                logger.warn { "localUpdateChannel for route handler was overriden" }
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

    infix fun SetSerializer<T>.withSerializer(build: WithSerializer<T>.() -> Unit) {
        val ws = WithSerializer(this.serializer)
        ws.build()
        this@CombinedRouteHandlerBuilder.withSerializer = ws
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

    @CombinedRouteConfigMarker
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

@CombinedRouteConfigMarker
class WithSerializer<T> internal constructor(internal val messageSerializer: MessageSerializer<T>) {

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

    @CombinedRouteConfigMarker
    class OnSide internal constructor() {

        internal var receiveMessage: MessageReceiver? = null

        fun receiveMessag(messageReceiver: MessageReceiver) {
            receiveMessage = messageReceiver
        }

    }

}

@CombinedRouteConfigMarker
class SetSerializer<T> internal constructor(val serializer: MessageSerializer<T>)

@CombinedRouteConfigMarker
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
annotation class SideRouteConfigMarker

@SideRouteConfigMarker
class SideRouteConfig internal constructor(private val device: FSBaseDevice) {

    internal val networkRouteHandlerMap = HashMap<Route, NetworkRouteHandler<*>>()

    internal val networkUpdateChannelMap = HashMap<Route, Channel<String?>>()

    fun add(additions: SideRouteConfig.() -> Unit) {
        this.additions()
    }

    fun route(vararg path: String): Route = listOf(*path)

    fun route(path: List<String>): Route = path

    fun <T> handler(
        isFullyBiDirectional: Boolean,
        build: SideRouteHandlerBuilder<T>.() -> Unit
    ): HandlerParams<T> {
        return HandlerParams(isFullyBiDirectional, build)
    }

    @Suppress("NAME_SHADOWING")
    infix fun <T> Route.to(handler: HandlerParams<T>) {
        val (isFullyBiDirectional, build) = handler

        val routeHandlerBuilder = SideRouteHandlerBuilder<T>()
        routeHandlerBuilder.build()

        val networkUpdateChannel = if (routeHandlerBuilder.receive != null) {
            Channel<String?>(10_000).also { networkUpdateChannelMap += this to it }
        } else {
            null
        }

        val currentHandler = networkRouteHandlerMap[this]
        if (currentHandler != null) {
            logger.warn { "Overriding side routing for route $this." }
        }

        networkRouteHandlerMap[this] = when (device) {
            is LocalDevice -> NetworkRouteHandler.Host(
                device,
                this,
                routeHandlerBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.send,
                routeHandlerBuilder.receive
            )
            is FSRemoteDevice -> NetworkRouteHandler.Remote(
                device,
                this,
                routeHandlerBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.send,
                routeHandlerBuilder.receive,
                isFullyBiDirectional
            )
            else -> throw IllegalStateException(
                "Concrete FSDevice must extend either LocalDevice or FSRemoteDevice"
            )
        }
    }

    data class HandlerParams<T> internal constructor(
        val isFullyBiDirectional: Boolean,
        val build: SideRouteHandlerBuilder<T>.() -> Unit
    )
}

@SideRouteConfigMarker
class SideRouteHandlerBuilder<T> internal constructor() {

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
        this@SideRouteHandlerBuilder.send = wuc.send
    }

}

@SideRouteConfigMarker
class SetUpdateChannel internal constructor()

enum class NullResolutionStrategy {
    PANIC, SKIP
}

@CombinedRouteConfigMarker
@SideRouteConfigMarker
class SideWithUpdateChannel {
    internal var send: Boolean = false

    fun send() {
        send = true
    }
}