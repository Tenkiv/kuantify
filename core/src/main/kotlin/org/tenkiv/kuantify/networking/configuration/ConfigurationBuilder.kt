package org.tenkiv.kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.device.*

typealias PingReceiver = () -> Unit
typealias MessageReceiver = (update: String) -> Unit

class NetworkConfig<D : KuantifyDevice> internal constructor(val device: D) {

    internal val networkRouteHandlers = ArrayList<NetworkRouteHandler<*>>()

    internal val networkUpdateChannelMap = HashMap<Route, Channel<String?>>()

    fun route(vararg path: String): Route = listOf(*path)

    fun route(path: List<String>): Route = path

    fun <T> handler(
        localUpdateChannel: ReceiveChannel<T>,
        build: RouteHandlerBuilder<T>.() -> Unit
    ): HandlerParams<T> {
        return HandlerParams(localUpdateChannel, build)
    }

    @Suppress("NAME_SHADOWING")
    infix fun <T> Route.to(handler: HandlerParams<T>) {
        val (localUpdateChannel, build) = handler
        val networkUpdateChannel = Channel<String?>(10_000)
        networkUpdateChannelMap += this to networkUpdateChannel

        val routeHandlerBuilder = RouteHandlerBuilder<T>()
        routeHandlerBuilder.build()

        val receiveUpdatesOnHost: UpdateReceiver? = buildHostUpdateReceiver(routeHandlerBuilder)

        val receiveUpdatesOnRemote: UpdateReceiver? = buildRemoteUpdateReceiver(routeHandlerBuilder)

        networkRouteHandlers += when (device) {
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
                routeHandlerBuilder.sendFromHost,
                receiveUpdatesOnRemote
            )
            else -> throw IllegalStateException(
                "Concrete KuantifyDevice must extend either LocalDevice or RemoteKuantifyDevice"
            )
        }
    }

    private fun <T> buildHostUpdateReceiver(routeHandlerBuilder: RouteHandlerBuilder<T>): UpdateReceiver? {
        if (routeHandlerBuilder.receivePingOnEither == null &&
            routeHandlerBuilder.receivePingOnHost == null &&
            routeHandlerBuilder.withSerializer?.receiveMessageOnEither == null &&
            routeHandlerBuilder.withSerializer?.receiveMessageOnHost == null
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
                }
            }
        }
    }

    private fun <T> buildRemoteUpdateReceiver(routeHandlerBuilder: RouteHandlerBuilder<T>): UpdateReceiver? {
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
        val build: RouteHandlerBuilder<T>.() -> Unit
    )
}

class RouteHandlerBuilder<T> internal constructor() {
    internal var serializeMessage: MessageSerializer<T>? = null

    internal var withSerializer: WithSerializer<T>? = null

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

    class WithSerializer<T> internal constructor(internal val messageSerializer: MessageSerializer<T>) {

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

    }
}