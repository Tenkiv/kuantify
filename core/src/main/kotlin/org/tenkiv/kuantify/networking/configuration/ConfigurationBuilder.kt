package org.tenkiv.kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.device.*

typealias PingReceiver = () -> Unit
typealias MessageReceiver = (update: String) -> Unit

private val logger = KotlinLogging.logger {}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Combined ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

@DslMarker
annotation class CombinedRouteConfigMarker

@CombinedRouteConfigMarker
class CombinedRouteConfig internal constructor(val device: KuantifyDevice) {

    internal val networkRouteHandlerMap = HashMap<Route, NetworkRouteHandler<*>>()

    internal val networkUpdateChannelMap = HashMap<Route, Channel<String?>>()

    fun route(vararg path: String): Route = listOf(*path)

    fun route(path: List<String>): Route = path

    fun <T> handler(
        localUpdateChannel: ReceiveChannel<T>,
        isFullyBiDirectional: Boolean,
        build: CombinedRouteHandlerBuilder<T>.() -> Unit
    ): HandlerParams<T> {
        return HandlerParams(localUpdateChannel, isFullyBiDirectional, build)
    }

    @Suppress("NAME_SHADOWING")
    infix fun <T> Route.to(handler: HandlerParams<T>) {
        val (localUpdateChannel, isFullyBiDirectional, build) = handler
        val networkUpdateChannel = Channel<String?>(10_000)
        networkUpdateChannelMap += this to networkUpdateChannel

        val routeHandlerBuilder = CombinedRouteHandlerBuilder<T>()
        routeHandlerBuilder.build()

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
                localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.sendFromHost,
                receiveUpdatesOnHost
            )
            is RemoteKuantifyDevice -> NetworkRouteHandler.Remote(
                device,
                this,
                localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.sendFromRemote,
                receiveUpdatesOnRemote,
                isFullyBiDirectional
            )
            else -> throw IllegalStateException(
                "Concrete KuantifyDevice must extend either LocalDevice or RemoteKuantifyDevice"
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
        val localUpdateChannel: ReceiveChannel<T>,
        val isFullyBiDirectional: Boolean,
        val build: CombinedRouteHandlerBuilder<T>.() -> Unit
    )
}

@CombinedRouteConfigMarker
class CombinedRouteHandlerBuilder<T> internal constructor() {
    internal var serializeMessage: MessageSerializer<T>? = null

    internal var withSerializer: WithSerializer<T>? = null

    internal var onRemote: OnSide? = null

    internal var onHost: OnSide? = null

    internal var sendFromRemote: Boolean = false

    internal var sendFromHost: Boolean = false

    internal var receivePingOnEither: PingReceiver? = null

    internal var receivePingOnRemote: PingReceiver? = null

    internal var receivePingOnHost: PingReceiver? = null

    fun serializeMessage(messageSerializer: MessageSerializer<T>): MessageSerializer<T> {
        serializeMessage = messageSerializer
        return messageSerializer
    }

    fun sendFromRemote() {
        sendFromRemote = true
    }

    fun sendFromHost() {
        sendFromHost = true
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

    infix fun MessageSerializer<T>.withSerializer(build: WithSerializer<T>.() -> Unit) {
        val ws = WithSerializer(this)
        ws.build()
        withSerializer = ws
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
class OnSide internal constructor() {

    internal var send: Boolean = false

    internal var receivePing: PingReceiver? = null

    fun send() {
        send = true
    }

    fun receivePing(pingReceiver: PingReceiver) {
        receivePing = pingReceiver
    }

}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
//   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Separate ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//

@DslMarker
annotation class SideRouteConfigMarker

@SideRouteConfigMarker
class SideRouteConfig<D : BaseKuantifyDevice> internal constructor(val device: D) {

    internal val networkRouteHandlerMap = HashMap<Route, NetworkRouteHandler<*>>()

    internal val networkUpdateChannelMap = HashMap<Route, Channel<String?>>()

    fun route(vararg path: String): Route = listOf(*path)

    fun route(path: List<String>): Route = path

    fun <T> handler(
        localUpdateChannel: ReceiveChannel<T>,
        isFullyBiDirectional: Boolean,
        build: SideRouteHandlerBuilder<T>.() -> Unit
    ): HandlerParams<T> {
        return HandlerParams(localUpdateChannel, isFullyBiDirectional, build)
    }

    @Suppress("NAME_SHADOWING")
    infix fun <T> Route.to(handler: HandlerParams<T>) {
        val (localUpdateChannel, isFullyBiDirectional, build) = handler
        val networkUpdateChannel = Channel<String?>(10_000)
        networkUpdateChannelMap += this to networkUpdateChannel

        val routeHandlerBuilder = SideRouteHandlerBuilder<T>()
        routeHandlerBuilder.build()

        val currentHandler = networkRouteHandlerMap[this]
        if (currentHandler != null) {
            logger.warn { "Overriding side routing for route $this." }
        }

        networkRouteHandlerMap[this] = when (device) {
            is LocalDevice -> NetworkRouteHandler.Host(
                device,
                this,
                localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.send,
                routeHandlerBuilder.receive
            )
            is RemoteKuantifyDevice -> NetworkRouteHandler.Remote(
                device,
                this,
                localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeMessage,
                routeHandlerBuilder.send,
                routeHandlerBuilder.receive,
                isFullyBiDirectional
            )
            else -> throw IllegalStateException(
                "Concrete KuantifyDevice must extend either LocalDevice or RemoteKuantifyDevice"
            )
        }
    }

    data class HandlerParams<T> internal constructor(
        val localUpdateChannel: ReceiveChannel<T>,
        val isFullyBiDirectional: Boolean,
        val build: SideRouteHandlerBuilder<T>.() -> Unit
    )
}

@SideRouteConfigMarker
class SideRouteHandlerBuilder<T> internal constructor() {

    internal var serializeMessage: MessageSerializer<T>? = null

    internal var send: Boolean = false

    internal var receive: UpdateReceiver? = null

    fun serializeMessage(messageSerializer: MessageSerializer<T>) {
        serializeMessage = messageSerializer
    }

    fun send() {
        send = true
    }

    fun receive(receiver: UpdateReceiver) {
        receive = receiver
    }

}