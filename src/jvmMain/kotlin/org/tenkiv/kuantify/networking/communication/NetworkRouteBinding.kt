/*
 * Copyright 2020 Tenkiv, Inc.
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

package org.tenkiv.kuantify.networking.communication

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.kuantify.hardware.device.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

public typealias UpdateReceiver<ST> = suspend (update: ST) -> Unit
public typealias MessageSerializer<MT, ST> = (update: MT) -> ST

private val logger = KotlinLogging.logger {}

public abstract class NetworkRouteBinding<MT, ST>(
    protected val networkCommunicator: NetworkCommunicator<ST>,
    internal val networkUpdateChannel: Channel<ST>?
) : CoroutineScope {

    public final override val coroutineContext: CoroutineContext
        get() = networkCommunicator.coroutineContext

    public abstract fun start()

    public companion object {

        internal fun throwIllegalStateSend(route: String, device: Device): Nothing {
            throw IllegalStateException(
                "Network binding for route: $route on device${device.uid}" +
                        " is configured to send but has no local update channel."
            )
        }

        internal fun throwIllegalStateReceive(route: String, device: Device): Nothing {
            throw IllegalStateException(
                "Network binding for route: $route on device${device.uid}" +
                        " is configured to receive but has no network update channel."
            )
        }

    }


    public data class Properties<MT, ST>(
        public val networkCommunicator: NetworkCommunicator<ST>,
        public val route: String,
        public val localUpdateChannel: ReceiveChannel<MT>?,
        public val networkUpdateChannel: Channel<ST>?,
        public val serializeMessage: MessageSerializer<MT, ST>?,
        public val sendUpdates: Boolean,
        public val receiveUpdate: UpdateReceiver<ST>?,
        public val serializedPing: ST
    )
}

public class RecursionPreventingRouteBinding<MT, ST>(
    networkCommunicator: NetworkCommunicator<ST>,
    private val route: String,
    private val localUpdateChannel: ReceiveChannel<MT>?,
    networkUpdateChannel: Channel<ST>?,
    private val serializeMessage: MessageSerializer<MT, ST>?,
    private val sendUpdates: Boolean,
    private val receiveUpdate: UpdateReceiver<ST>?,
    private val serializedPing: ST
) : NetworkRouteBinding<MT, ST>(networkCommunicator, networkUpdateChannel) {

    public constructor(props: Properties<MT, ST>) :
            this(
                props.networkCommunicator,
                props.route,
                props.localUpdateChannel,
                props.networkUpdateChannel,
                props.serializeMessage,
                props.sendUpdates,
                props.receiveUpdate,
                props.serializedPing
            )

    private val ignoreNextUpdate = AtomicBoolean(false)

    public override fun start() {
        // Send
        if (sendUpdates) {
            launch {
                localUpdateChannel?.consumeEach {
                    if (!ignoreNextUpdate.get()) {
                        val message = serializeMessage?.invoke(it) ?: serializedPing
                        networkCommunicator._sendMessage(route, message)
                    } else {
                        ignoreNextUpdate.set(false)
                    }
                } ?: throwIllegalStateSend(route, networkCommunicator.device)
            }
        }

        // Receive
        if (receiveUpdate != null) {
            launch {
                networkUpdateChannel?.consumeEach {
                    ignoreNextUpdate.set(true)
                    receiveUpdate.invoke(it)
                } ?: throwIllegalStateReceive(route, networkCommunicator.device)
            }
        }
    }

    public override fun toString(): String = """
        RecursionPreventingNetworkCommunicator(route=$route, serializedPing=$serializedPing)
    """.trimIndent()
}

public class StandardRouteBinding<MT, ST>(
    networkCommunicator: NetworkCommunicator<ST>,
    private val route: String,
    private val localUpdateChannel: ReceiveChannel<MT>?,
    networkUpdateChannel: Channel<ST>?,
    private val serializeMessage: MessageSerializer<MT, ST>?,
    private val sendUpdates: Boolean,
    private val receiveUpdate: UpdateReceiver<ST>?,
    private val serializedPing: ST
) : NetworkRouteBinding<MT, ST>(networkCommunicator, networkUpdateChannel) {

    public constructor(props: Properties<MT, ST>) :
            this(
                props.networkCommunicator,
                props.route,
                props.localUpdateChannel,
                props.networkUpdateChannel,
                props.serializeMessage,
                props.sendUpdates,
                props.receiveUpdate,
                props.serializedPing
            )

    public override fun start() {
        // Send
        if (sendUpdates) {
            launch {
                localUpdateChannel?.consumeEach {
                    val message = serializeMessage?.invoke(it) ?: serializedPing
                    networkCommunicator._sendMessage(route, message)
                } ?: throwIllegalStateSend(route, networkCommunicator.device)
            }
        }

        // Receive
        if (receiveUpdate != null) {
            launch {
                networkUpdateChannel?.consumeEach {
                    receiveUpdate.invoke(it)
                } ?: throwIllegalStateReceive(route, networkCommunicator.device)
            }
        }
    }

    public override fun toString(): String = """
        StandardRouteBinding(route=$route, serializedPing=$serializedPing)
    """.trimIndent()

}