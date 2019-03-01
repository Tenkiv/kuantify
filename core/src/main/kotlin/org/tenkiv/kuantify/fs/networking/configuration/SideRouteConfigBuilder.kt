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

package org.tenkiv.kuantify.fs.networking.configuration

import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.device.*

typealias MessageReceiver = suspend (update: String) -> Unit

private val logger = KotlinLogging.logger {}

internal fun Path.toPathString(): String {
    var result = ""
    forEachIndexed { index, value ->
        val append = if (index != lastIndex) "/" else ""
        result += "$value$append"
    }
    return result
}

@DslMarker
annotation class SideRouteMarker

internal class SideRouteConfig(private val device: FSBaseDevice) {

    val networkRouteBindingMap = HashMap<String, NetworkRouteBinding<*>>()

    val baseRoute: SideNetworkRouting
        get() = SideNetworkRouting(
            this,
            emptyList()
        )

    @Suppress("NAME_SHADOWING")
    fun <T> addRouteBinding(
        path: Path,
        isFullyBiDirectional: Boolean,
        build: SideRouteBindingBuilder<T>.() -> Unit
    ) {
        val path = path.toPathString()

        val routeBindingBuilder = SideRouteBindingBuilder<T>()
        routeBindingBuilder.build()

        val networkUpdateChannel = if (routeBindingBuilder.receive != null) {
            Channel<String?>(10_000)
        } else {
            null
        }

        val currentBinding = networkRouteBindingMap[path]
        if (currentBinding != null) {
            logger.warn { "Overriding side route binding for route $path." }
        }

        networkRouteBindingMap[path] = when (device) {
            is LocalDevice -> NetworkRouteBinding.Host(
                device,
                path,
                routeBindingBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeBindingBuilder.serializeMessage,
                routeBindingBuilder.send,
                routeBindingBuilder.receive
            )
            is FSRemoteDevice -> NetworkRouteBinding.Remote(
                device,
                path,
                routeBindingBuilder.localUpdateChannel,
                networkUpdateChannel,
                routeBindingBuilder.serializeMessage,
                routeBindingBuilder.send,
                routeBindingBuilder.receive,
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

enum class NullResolutionStrategy {
    PANIC, SKIP
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