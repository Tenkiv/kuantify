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
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.networking.communication.*

typealias Path = List<String>
typealias Ping = Unit

private val logger = KotlinLogging.logger {}

@DslMarker
annotation class SideRouteMarker

class SideRouteConfig<ST>(
    private val networkCommunicator: NetworkCommunicator<ST>,
    private val serializedPing: ST,
    private val formatPath: (Path) -> String
) {

    val networkRouteBindingMap = HashMap<String, NetworkRouteBinding<*, ST>>()

    val baseRoute: SideNetworkRouting<ST>
        get() = SideNetworkRouting(this, emptyList())

    private val remoteConnectionCommunicator: Boolean =
        (networkCommunicator as? RemoteNetworkCommunicator)?.communicationMode != CommunicationMode.NO_CONNECTION

    @Suppress("NAME_SHADOWING")
    fun <MT> addRouteBinding(
        path: Path,
        recursiveSynchronizer: Boolean, //TODO: Rename this
        build: SideRouteBindingBuilder<MT, ST>.() -> Unit
    ) {
        val path = formatPath(path)

        val routeBindingBuilder = SideRouteBindingBuilder<MT, ST>()
        routeBindingBuilder.build()

        val networkUpdateChannel = if (routeBindingBuilder.receive != null) {
            Channel<ST>(Channel.UNLIMITED)
        } else {
            null
        }

        val currentBinding = networkRouteBindingMap[path]
        if (currentBinding != null) {
            logger.warn { "Overriding side route binding for route $path." }
        }

        fun standardRouteBinding() = StandardRouteBinding(
            networkCommunicator,
            path,
            routeBindingBuilder.localUpdateChannel,
            networkUpdateChannel,
            routeBindingBuilder.serializeMessage,
            routeBindingBuilder.send,
            routeBindingBuilder.receive,
            serializedPing
        )

        fun recursionPreventingRouteBinding() = RecursionPreventingRouteBinding(
            networkCommunicator,
            path,
            routeBindingBuilder.localUpdateChannel,
            networkUpdateChannel,
            routeBindingBuilder.serializeMessage,
            routeBindingBuilder.send,
            routeBindingBuilder.receive,
            serializedPing
        )

        networkRouteBindingMap[path] = if (remoteConnectionCommunicator && recursiveSynchronizer) {
            recursionPreventingRouteBinding()
        } else {
            standardRouteBinding()
        }
    }

}

@Suppress("NAME_SHADOWING")
@SideRouteMarker
class SideNetworkRouting<ST> internal constructor(private val config: SideRouteConfig<ST>, private val path: Path) {

    fun <MT> bind(
        vararg path: String,
        recursiveSynchronizer: Boolean = false,
        build: SideRouteBindingBuilder<MT, ST>.() -> Unit
    ) {
        bind(path.toList(), recursiveSynchronizer, build)
    }

    fun <MT> bind(
        path: Path,
        recursiveSynchronizer: Boolean = false,
        build: SideRouteBindingBuilder<MT, ST>.() -> Unit
    ) {
        val path = this.path + path

        config.addRouteBinding(
            path,
            recursiveSynchronizer,
            build = build
        )
    }

    fun route(vararg path: String, build: SideNetworkRouting<ST>.() -> Unit) {
        route(path.toList(), build)
    }


    fun route(path: Path, build: SideNetworkRouting<ST>.() -> Unit) {
        val path = this.path + path

        SideNetworkRouting(config, path).apply(build)
    }
}

@SideRouteMarker
class SideRouteBindingBuilder<MT, ST> internal constructor() {

    internal var localUpdateChannel: ReceiveChannel<MT>? = null

    internal var serializeMessage: MessageSerializer<MT, ST>? = null

    internal var send: Boolean = false

    @PublishedApi
    internal var receive: UpdateReceiver<ST>? = null

    fun serializeMessage(messageSerializer: MessageSerializer<MT, ST>) {
        serializeMessage = messageSerializer
    }

    fun setLocalUpdateChannel(channel: ReceiveChannel<MT>): SetUpdateChannel {
        localUpdateChannel = channel
        return SetUpdateChannel()
    }

    fun receive(receiver: UpdateReceiver<ST>) {
        receive = receiver
    }

    infix fun SetUpdateChannel.withUpdateChannel(build: SideWithUpdateChannel.() -> Unit) {
        val wuc = SideWithUpdateChannel()
        wuc.build()
        this@SideRouteBindingBuilder.send = wuc.send
    }
}

@SideRouteMarker
class SetUpdateChannel internal constructor()

@CombinedRouteMarker
@SideRouteMarker
class SideWithUpdateChannel {
    internal var send: Boolean = false

    fun send() {
        send = true
    }
}