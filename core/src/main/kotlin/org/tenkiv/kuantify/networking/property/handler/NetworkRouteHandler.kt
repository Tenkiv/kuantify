package org.tenkiv.kuantify.networking.property.handler

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

typealias Route = List<String>
typealias UpdateReceiver<R> = (R, update: String) -> Unit
typealias UpdateSerializer<T> = (update: T) -> String

internal sealed class NetworkRouteHandler<R : Any, T>(protected val device: KuantifyDevice) : CoroutineScope {

    @Volatile
    protected var job = Job(coroutineContext[Job])

    final override val coroutineContext: CoroutineContext
        get() = device.coroutineContext + job

    open fun start(job: Job) {
        this.job = job
    }

    internal class Host<R : Any, T> internal constructor(
        device: LocalDevice,
        private val route: Route,
        private val localReceiver: R,
        private val localUpdateChannel: ReceiveChannel<T>,
        private val networkUpdateChannel: ReceiveChannel<String>,
        private val serializeUpdate: UpdateSerializer<T>?,
        private val sendUpdatesFromHost: Boolean,
        private val receiveUpdateOnHost: UpdateReceiver<R>?
    ) : NetworkRouteHandler<R, T>(device) {

        override fun start(job: Job) {
            super.start(job)
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

    internal class Remote<R : Any, T> internal constructor(
        device: RemoteKuantifyDevice,
        private val route: Route,
        private val localReceiver: R,
        private val localUpdateChannel: ReceiveChannel<T>,
        private val networkUpdateChannel: ReceiveChannel<String>,
        private val serializeUpdate: UpdateSerializer<T>?,
        private val sendUpdatesFromRemote: Boolean,
        private val sendUpdatesFromHost: Boolean,
        private val receiveUpdateOnRemote: UpdateReceiver<R>?
    ) : NetworkRouteHandler<R, T>(device) {

        private val fullyBiDirectional get() = sendUpdatesFromHost && sendUpdatesFromRemote

        private val ignoreNextUpdate = AtomicBoolean(false)

        override fun start(job: Job) {
            super.start(job)
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

class NetworkCommunicatorBuilder internal constructor(val device: KuantifyDevice) {

    internal val networkRouteHandlers = ArrayList<NetworkRouteHandler<*, *>>()

    internal val networkUpdateChannelMap = HashMap<Route, Channel<String>>()

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

class RouteHandlerBuilder<R : Any, T> internal constructor() {
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