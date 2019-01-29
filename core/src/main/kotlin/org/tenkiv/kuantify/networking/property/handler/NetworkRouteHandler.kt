package org.tenkiv.kuantify.networking.property.handler

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import java.util.concurrent.atomic.*

typealias Route = List<String>
typealias UpdateReceiver<R> = (R, update: String) -> Unit
typealias UpdateSerializer<T> = (update: T) -> String

sealed class NetworkRouteHandler<R : Any, T>(scope: CoroutineScope) : CoroutineScope by scope {

    class Host<R : Any, T>(
        scope: CoroutineScope,
        val device: LocalDevice,
        val route: Route,
        val localReceiver: R,
        val localUpdateChannel: ReceiveChannel<T>,
        val networkUpdateChannel: ReceiveChannel<String>,
        val serializeUpdate: UpdateSerializer<T>?,
        val sendUpdatesFromHost: Boolean,
        val receiveUpdateOnHost: UpdateReceiver<R>?
    ) : NetworkRouteHandler<R, T>(scope) {

        fun start() {
            // Send
            if (sendUpdatesFromHost) {
                launch {
                    localUpdateChannel.consumeEach {
                        val payload = serializeUpdate?.invoke(it)
                        device.sendMessage(route, payload)
                    }
                }
            }

            // Receive
            if (receiveUpdateOnHost != null) {
                launch {
                    networkUpdateChannel.consumeEach {
                        receiveUpdateOnHost.invoke(localReceiver, it)
                    }
                }
            }
        }

    }

    class Remote<R : Any, T>(
        scope: CoroutineScope,
        val device: RemoteKuantifyDevice,
        val route: Route,
        val localReceiver: R,
        val localUpdateChannel: ReceiveChannel<T>,
        val networkUpdateChannel: ReceiveChannel<String>,
        val serializeUpdate: UpdateSerializer<T>?,
        val sendUpdatesFromRemote: Boolean,
        val sendUpdatesFromHost: Boolean,
        val receiveUpdateOnRemote: UpdateReceiver<R>?
    ) : NetworkRouteHandler<R, T>(scope) {

        private val fullyBiDirectional get() = sendUpdatesFromHost && sendUpdatesFromRemote

        private val ignoreNextUpdate = AtomicBoolean(false)

        fun start() {
            // Send
            if (sendUpdatesFromRemote) {
                if (fullyBiDirectional) {
                    launch {
                        localUpdateChannel.consumeEach {
                            if (!ignoreNextUpdate.get()) {
                                val payload = serializeUpdate?.invoke(it)
                                device.sendMessage(route, payload)
                            } else {
                                ignoreNextUpdate.set(false)
                            }
                        }
                    }
                } else {
                    launch {
                        localUpdateChannel.consumeEach {
                            val payload = serializeUpdate?.invoke(it)
                            device.sendMessage(route, payload)
                        }
                    }
                }
            }

            // Receive
            if (receiveUpdateOnRemote != null) {
                if (fullyBiDirectional) {
                    launch {
                        networkUpdateChannel.consumeEach {
                            ignoreNextUpdate.set(true)
                            receiveUpdateOnRemote.invoke(localReceiver, it)
                        }
                    }
                } else {
                    launch {
                        networkUpdateChannel.consumeEach {
                            receiveUpdateOnRemote.invoke(localReceiver, it)
                        }
                    }
                }
            }
        }

    }

}

class NetworkCommunicatorBuilder(val device: KuantifyDevice) {

    val networkRouteHandlers = ArrayList<NetworkRouteHandler<*, *>>()

    val networkUpdateChannelMap = HashMap<Route, Channel<String>>()

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
        val networkUpdateChannel = Channel<String>(10_000)
        networkUpdateChannelMap += this to networkUpdateChannel

        val routHandlerBuilder = RouteHandlerBuilder<R, T>()
        routHandlerBuilder.build()

        val receiveUpdatesOnHost: UpdateReceiver<R> = { receiver, update ->
            routHandlerBuilder.receiveUpdatesOnEither?.invoke(receiver, update)
            routHandlerBuilder.receiveUpdatesOnHost?.invoke(receiver, update)
        }

        val receiveUpdatesOnRemote: UpdateReceiver<R> = { receiver, update ->
            routHandlerBuilder.receiveUpdatesOnEither?.invoke(receiver, update)
            routHandlerBuilder.receiveUpdatesOnRemote?.invoke(receiver, update)
        }

        networkRouteHandlers += when (device) {
            is LocalDevice -> NetworkRouteHandler.Host(
                GlobalScope,
                device,
                this,
                localReceiver,
                localUpdateChannel,
                networkUpdateChannel,
                routHandlerBuilder.serializeUpdate,
                routHandlerBuilder.sendUpdatesFromHost,
                receiveUpdatesOnHost
            )
            is RemoteKuantifyDevice -> NetworkRouteHandler.Remote(
                GlobalScope,
                device,
                this,
                localReceiver,
                localUpdateChannel,
                networkUpdateChannel,
                routHandlerBuilder.serializeUpdate,
                routHandlerBuilder.sendUpdatesFromRemote,
                routHandlerBuilder.sendUpdatesFromHost,
                receiveUpdatesOnRemote
            )
        }
    }

    data class HandlerParams<R : Any, T> internal constructor(
        val localReceiver: R,
        val localUpdateChannel: ReceiveChannel<T>,
        val build: RouteHandlerBuilder<R, T>.() -> Unit
    )

}

class RouteHandlerBuilder<R : Any, T> {
    internal var serializeUpdate: UpdateSerializer<T>? = null

    internal var sendUpdatesFromRemote: Boolean = false

    internal var sendUpdatesFromHost: Boolean = false

    internal var receiveUpdatesOnEither: UpdateReceiver<R>? = null

    internal var receiveUpdatesOnRemote: UpdateReceiver<R>? = null

    internal var receiveUpdatesOnHost: UpdateReceiver<R>? = null

    fun serializeUpdate(updateSerializer: UpdateSerializer<T>) {
        serializeUpdate = updateSerializer
    }

    fun sendUpdatesFromRemote() {
        sendUpdatesFromRemote = true
    }

    fun sendUdatesFromHost() {
        sendUpdatesFromHost = true
    }

    fun receiveUpdatesOnEither(updateReceiver: UpdateReceiver<R>) {
        receiveUpdatesOnEither = updateReceiver
    }

    fun receiveUpdatesOnRemote(updateReceiver: UpdateReceiver<R>) {
        receiveUpdatesOnRemote = updateReceiver
    }

    fun receiveUpdatesOnHost(updateReceiver: UpdateReceiver<R>) {
        receiveUpdatesOnHost = updateReceiver
    }
}