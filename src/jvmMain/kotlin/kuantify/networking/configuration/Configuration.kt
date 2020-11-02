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

package kuantify.networking.configuration

import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kuantify.lib.*

public typealias Path = List<String>
public typealias Ping = Unit

public typealias MessageSerializer<BoundT, SerialT> = (update: BoundT) -> SerialT

@DslMarker
internal annotation class NetworkingDsl

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Send ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
internal sealed class MessageSender<BoundT, SerialT : Any> {
    abstract val serialize: MessageSerializer<BoundT, SerialT>

    // This function is implemented here instead of in the subclasses to allow it to be inlined.
    suspend inline fun onEachMessage(crossinline action: suspend (update: BoundT) -> Unit) {
        when(this) {
            is Flow -> this.flow.collect(action)
            is Channel -> this.channel.consumingOnEach { update -> action(update) }
        }
    }

    data class Flow<BoundT, SerialT : Any>(
        val flow: kotlinx.coroutines.flow.Flow<BoundT>,
        override val serialize: MessageSerializer<BoundT, SerialT>
    ) : MessageSender<BoundT, SerialT>()

    data class Channel<BoundT, SerialT : Any>(
        val channel: ReceiveChannel<BoundT>,
        override val serialize: MessageSerializer<BoundT, SerialT>
    ) : MessageSender<BoundT, SerialT>()

}

//▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Receive ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
public typealias ReceiveMessage<SerialT> = suspend (update: SerialT) -> Unit
public typealias ReceivePing = suspend () -> Unit

@PublishedApi
internal data class MessageReceiver<SerialT : Any>(
    val channel: Channel<SerialT>,
    val receiveOp: ReceiveMessage<SerialT>
)

@PublishedApi
internal data class PingReceiver(
    val channel: Channel<Ping>,
    val receiveOp: ReceivePing
)