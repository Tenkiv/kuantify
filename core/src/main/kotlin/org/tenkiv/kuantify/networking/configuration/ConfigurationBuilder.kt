package org.tenkiv.kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.device.*

typealias PingReceiver<R> = (R) -> Unit
typealias MessageReceiver<R> = (R, update: String) -> Unit

class NetworkCommunicatorBuilder internal constructor(val device: KuantifyDevice) {

    internal val networkRouteHandlers = ArrayList<NetworkRouteHandler<*, *>>()

    internal val networkUpdateChannelMap = HashMap<Route, Channel<String?>>()

    fun route(vararg components: String): Route = listOf(*components)

    fun <R : Any, T> handler(
        localReceiver: R,
        localUpdateChannel: ReceiveChannel<T>,
        build: RouteHandlerBuilder<R, T>.() -> Unit
    ): HandlerParams<R, T> {
        return HandlerParams(localReceiver, localUpdateChannel, build)
    }

    @Suppress("NAME_SHADOWING")
    infix fun <R : Any, T> Route.to(handler: HandlerParams<R, T>) {
        val (localReceiver, localUpdateChannel, build) = handler
        val networkUpdateChannel = Channel<String?>(10_000)
        networkUpdateChannelMap += this to networkUpdateChannel

        val routeHandlerBuilder = RouteHandlerBuilder<R, T>()
        routeHandlerBuilder.build()

        val receiveUpdatesOnHost: UpdateReceiver<R>? = buildHostUpdateReceiver(routeHandlerBuilder)

        val receiveUpdatesOnRemote: UpdateReceiver<R>? = buildRemoteUpdateReceiver(routeHandlerBuilder)

        networkRouteHandlers += when (device) {
            is LocalDevice -> NetworkRouteHandler.Host(
                device,
                this,
                localReceiver,
                localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeUpdates,
                routeHandlerBuilder.sendUpdatesFromHost,
                receiveUpdatesOnHost
            )
            is RemoteKuantifyDevice -> NetworkRouteHandler.Remote(
                device,
                this,
                localReceiver,
                localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeUpdates,
                routeHandlerBuilder.sendUpdatesFromRemote,
                routeHandlerBuilder.sendUpdatesFromHost,
                receiveUpdatesOnRemote
            )
        }
    }

    private fun <R : Any, T> buildHostUpdateReceiver(routeHandlerBuilder: RouteHandlerBuilder<R, T>):
            UpdateReceiver<R>? {
        if (routeHandlerBuilder.receivePingsOnEither == null &&
            routeHandlerBuilder.receivePingsOnHost == null &&
            routeHandlerBuilder.withSerializer?.receiveMessagesOnEither == null &&
            routeHandlerBuilder.withSerializer?.receiveMessagesOnHost == null
        ) {
            return null
        } else {
            return { receiver, update ->
                if (update != null) {
                    routeHandlerBuilder.withSerializer?.receiveMessagesOnEither?.invoke(receiver, update)
                    routeHandlerBuilder.withSerializer?.receiveMessagesOnHost?.invoke(receiver, update)
                } else {
                    routeHandlerBuilder.receivePingsOnEither?.invoke(receiver)
                    routeHandlerBuilder.receivePingsOnHost?.invoke(receiver)
                }
            }
        }
    }

    private fun <R : Any, T> buildRemoteUpdateReceiver(routeHandlerBuilder: RouteHandlerBuilder<R, T>):
            UpdateReceiver<R>? {
        if (routeHandlerBuilder.receivePingsOnEither == null &&
            routeHandlerBuilder.receivePingsOnRemote == null &&
            routeHandlerBuilder.withSerializer?.receiveMessagesOnEither == null &&
            routeHandlerBuilder.withSerializer?.receiveMessagesOnRemote == null
        ) {
            return null
        } else {
            return { receiver, update ->
                if (update != null) {
                    routeHandlerBuilder.withSerializer?.receiveMessagesOnEither?.invoke(receiver, update)
                    routeHandlerBuilder.withSerializer?.receiveMessagesOnRemote?.invoke(receiver, update)
                } else {
                    routeHandlerBuilder.receivePingsOnEither?.invoke(receiver)
                    routeHandlerBuilder.receivePingsOnRemote?.invoke(receiver)
                }
            }
        }
    }

    data class HandlerParams<R : Any, T> internal constructor(
        val localReceiver: R,
        val localUpdateChannel: ReceiveChannel<T>,
        val build: RouteHandlerBuilder<R, T>.() -> Unit
    )
}

class RouteHandlerBuilder<R : Any, T> internal constructor() {
    internal var serializeUpdates: MessageSerializer<T>? = null

    internal var withSerializer: WithSerializer<R, T>? = null

    internal var sendUpdatesFromRemote: Boolean = false

    internal var sendUpdatesFromHost: Boolean = false

    internal var receivePingsOnEither: PingReceiver<R>? = null

    internal var receivePingsOnRemote: PingReceiver<R>? = null

    internal var receivePingsOnHost: PingReceiver<R>? = null

    fun serializeUpdates(messageSerializer: MessageSerializer<T>): MessageSerializer<T> {
        serializeUpdates = messageSerializer
        return messageSerializer
    }

    fun sendFromRemote() {
        sendUpdatesFromRemote = true
    }

    fun sendFromHost() {
        sendUpdatesFromHost = true
    }

    fun receivePingsOnEither(pingReceiver: PingReceiver<R>) {
        receivePingsOnEither = pingReceiver
    }

    fun receivePingsOnRemote(pingReceiver: PingReceiver<R>) {
        receivePingsOnRemote = pingReceiver
    }

    fun receivePingsOnHost(pingReceiver: PingReceiver<R>) {
        receivePingsOnHost = pingReceiver
    }

    infix fun MessageSerializer<T>.withSerializer(build: WithSerializer<R, T>.() -> Unit) {
        val ws = WithSerializer<R, T>(this)
        ws.build()
        withSerializer = ws
    }

    class WithSerializer<R : Any, T> internal constructor(internal val messageSerializer: MessageSerializer<T>) {

        internal var receiveMessagesOnEither: MessageReceiver<R>? = null

        internal var receiveMessagesOnRemote: MessageReceiver<R>? = null

        internal var receiveMessagesOnHost: MessageReceiver<R>? = null

        fun receiveMessagesOnEither(messageReceiver: MessageReceiver<R>) {
            receiveMessagesOnEither = messageReceiver
        }

        fun receiveMessagesOnRemote(messageReceiver: MessageReceiver<R>) {
            receiveMessagesOnRemote = messageReceiver
        }

        fun receiveMessagesOnHost(messageReceiver: MessageReceiver<R>) {
            receiveMessagesOnHost = messageReceiver
        }

    }
}