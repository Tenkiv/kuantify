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
 */

package org.tenkiv.kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.networking.communication.*

public typealias Path = List<String>
public typealias Ping = Unit

@PublishedApi
internal val sideRouteConfigBuilderLogger = KotlinLogging.logger {}

public class SideRouteConfig<ST>(
    @PublishedApi internal val networkCommunicator: NetworkCommunicator<ST>,
    @PublishedApi internal val serializedPing: ST,
    @PublishedApi internal val formatPath: (Path) -> String
) {

    public val networkRouteBindingMap: HashMap<String, NetworkRouteBinding<*, ST>> = HashMap()

    public val baseRoute: SideNetworkRouting<ST>
        get() = SideNetworkRouting(this, emptyList())

    @PublishedApi
    internal val remoteConnectionCommunicator: Boolean =
        (networkCommunicator as? RemoteNetworkCommunicator)?.communicationMode != CommunicationMode.NO_CONNECTION

    @Suppress("NAME_SHADOWING")
    @PublishedApi
    internal inline fun <MT> addRouteBinding(
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
            sideRouteConfigBuilderLogger.warn { "Overriding side route binding for route $path." }
        }

        val standardRouteBinding by lazy(LazyThreadSafetyMode.NONE) {
            StandardRouteBinding(
                networkCommunicator,
                path,
                routeBindingBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeBindingBuilder.serializeMessage,
                routeBindingBuilder.send,
                routeBindingBuilder.receive,
                serializedPing
            )
        }

        val recursionPreventingRouteBinding by lazy(LazyThreadSafetyMode.NONE) {
            RecursionPreventingRouteBinding(
                networkCommunicator,
                path,
                routeBindingBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeBindingBuilder.serializeMessage,
                routeBindingBuilder.send,
                routeBindingBuilder.receive,
                serializedPing
            )
        }

        networkRouteBindingMap[path] = if (remoteConnectionCommunicator && recursiveSynchronizer) {
            recursionPreventingRouteBinding
        } else {
            standardRouteBinding
        }
    }

}

@Suppress("NAME_SHADOWING")
@NetworkingDsl
public class SideNetworkRouting<ST> @PublishedApi internal constructor(
    @PublishedApi internal val config: SideRouteConfig<ST>,
    @PublishedApi internal val path: Path
) {

    @NetworkingDsl
    public inline fun <MT> bind(
        vararg path: String,
        recursiveSynchronizer: Boolean = false,
        build: SideRouteBindingBuilder<MT, ST>.() -> Unit
    ) {
        bind(path.toList(), recursiveSynchronizer, build)
    }

    @NetworkingDsl
    public inline fun <MT> bind(
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

    @NetworkingDsl
    public inline fun route(vararg path: String, build: SideNetworkRouting<ST>.() -> Unit) {
        route(path.toList(), build)
    }

    @NetworkingDsl
    public inline fun route(path: Path, build: SideNetworkRouting<ST>.() -> Unit) {
        val path = this.path + path

        SideNetworkRouting(config, path).apply(build)
    }
}

@NetworkingDsl
public class SideRouteBindingBuilder<MT, ST> @PublishedApi internal constructor() {

    @PublishedApi
    internal var localUpdateChannel: ReceiveChannel<MT>? = null

    @PublishedApi
    internal var serializeMessage: MessageSerializer<MT, ST>? = null

    @PublishedApi
    internal var send: Boolean = false

    @PublishedApi
    internal var receive: UpdateReceiver<ST>? = null

    @NetworkingDsl
    public fun serializeMessage(messageSerializer: MessageSerializer<MT, ST>) {
        serializeMessage = messageSerializer
    }

    @NetworkingDsl
    public fun setLocalUpdateChannel(channel: ReceiveChannel<MT>): SetUpdateChannel {
        localUpdateChannel = channel
        return SetUpdateChannel()
    }

    @NetworkingDsl
    public fun receive(receiver: UpdateReceiver<ST>) {
        receive = receiver
    }

    @NetworkingDsl
    public inline infix fun SetUpdateChannel.withUpdateChannel(build: SideWithUpdateChannel.() -> Unit) {
        this@SideRouteBindingBuilder.send = SideWithUpdateChannel().apply(build).send
    }
}

@NetworkingDsl
public class SetUpdateChannel internal constructor()

@NetworkingDsl
public class SideWithUpdateChannel {
    @PublishedApi
    internal var send: Boolean = false

    @NetworkingDsl
    public fun send() {
        send = true
    }
}