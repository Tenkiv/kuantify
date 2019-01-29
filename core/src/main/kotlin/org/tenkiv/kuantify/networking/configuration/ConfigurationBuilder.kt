package org.tenkiv.kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.device.*

typealias PingReceiver = () -> Unit
typealias MessageReceiver = (update: String) -> Unit

class NetworkCommunicatorBuilder internal constructor(val device: KuantifyDevice) {

    internal val networkRouteHandlers = ArrayList<NetworkRouteHandler<*>>()

    internal val networkUpdateChannelMap = HashMap<Route, Channel<String?>>()

    fun route(vararg components: String): Route = listOf(*components)

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
                routeHandlerBuilder.serializeUpdates,
                routeHandlerBuilder.sendUpdatesFromHost,
                receiveUpdatesOnHost
            )
            is RemoteKuantifyDevice -> NetworkRouteHandler.Remote(
                device,
                this,
                localUpdateChannel,
                networkUpdateChannel,
                routeHandlerBuilder.serializeUpdates,
                routeHandlerBuilder.sendUpdatesFromRemote,
                routeHandlerBuilder.sendUpdatesFromHost,
                receiveUpdatesOnRemote
            )
        }
    }

    private fun <T> buildHostUpdateReceiver(routeHandlerBuilder: RouteHandlerBuilder<T>):
            UpdateReceiver? {
        if (routeHandlerBuilder.receivePingsOnEither == null &&
            routeHandlerBuilder.receivePingsOnHost == null &&
            routeHandlerBuilder.withSerializer?.receiveMessagesOnEither == null &&
            routeHandlerBuilder.withSerializer?.receiveMessagesOnHost == null
        ) {
            return null
        } else {
            return { update ->
                if (update != null) {
                    routeHandlerBuilder.withSerializer?.receiveMessagesOnEither?.invoke(update)
                    routeHandlerBuilder.withSerializer?.receiveMessagesOnHost?.invoke(update)
                } else {
                    routeHandlerBuilder.receivePingsOnEither?.invoke()
                    routeHandlerBuilder.receivePingsOnHost?.invoke()
                }
            }
        }
    }

    private fun <T> buildRemoteUpdateReceiver(routeHandlerBuilder: RouteHandlerBuilder<T>):
            UpdateReceiver? {
        if (routeHandlerBuilder.receivePingsOnEither == null &&
            routeHandlerBuilder.receivePingsOnRemote == null &&
            routeHandlerBuilder.withSerializer?.receiveMessagesOnEither == null &&
            routeHandlerBuilder.withSerializer?.receiveMessagesOnRemote == null
        ) {
            return null
        } else {
            return { update ->
                if (update != null) {
                    routeHandlerBuilder.withSerializer?.receiveMessagesOnEither?.invoke(update)
                    routeHandlerBuilder.withSerializer?.receiveMessagesOnRemote?.invoke(update)
                } else {
                    routeHandlerBuilder.receivePingsOnEither?.invoke()
                    routeHandlerBuilder.receivePingsOnRemote?.invoke()
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
    internal var serializeUpdates: MessageSerializer<T>? = null

    internal var withSerializer: WithSerializer<T>? = null

    internal var sendUpdatesFromRemote: Boolean = false

    internal var sendUpdatesFromHost: Boolean = false

    internal var receivePingsOnEither: PingReceiver? = null

    internal var receivePingsOnRemote: PingReceiver? = null

    internal var receivePingsOnHost: PingReceiver? = null

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

    fun receivePingsOnEither(pingReceiver: PingReceiver) {
        receivePingsOnEither = pingReceiver
    }

    fun receivePingsOnRemote(pingReceiver: PingReceiver) {
        receivePingsOnRemote = pingReceiver
    }

    fun receivePingsOnHost(pingReceiver: PingReceiver) {
        receivePingsOnHost = pingReceiver
    }

    infix fun MessageSerializer<T>.withSerializer(build: WithSerializer<T>.() -> Unit) {
        val ws = WithSerializer(this)
        ws.build()
        withSerializer = ws
    }

    class WithSerializer<T> internal constructor(internal val messageSerializer: MessageSerializer<T>) {

        internal var receiveMessagesOnEither: MessageReceiver? = null

        internal var receiveMessagesOnRemote: MessageReceiver? = null

        internal var receiveMessagesOnHost: MessageReceiver? = null

        fun receiveMessagesOnEither(messageReceiver: MessageReceiver) {
            receiveMessagesOnEither = messageReceiver
        }

        fun receiveMessagesOnRemote(messageReceiver: MessageReceiver) {
            receiveMessagesOnRemote = messageReceiver
        }

        fun receiveMessagesOnHost(messageReceiver: MessageReceiver) {
            receiveMessagesOnHost = messageReceiver
        }

    }
}