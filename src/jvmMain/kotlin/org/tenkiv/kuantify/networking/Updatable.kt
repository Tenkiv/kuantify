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

package org.tenkiv.kuantify.networking

import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.trackable.*

/**
 * An [Updatable] meant to keep sync both ways between a remote client and a host on the remote side.
 * It's value will not be updated immediately when set, only once it receives confirmation of the updated from the host.
 */
@KuantifyComponentBuilder
public class RemoteSyncUpdatable<T : Any> : Updatable<T> {
    private val broadcastChannel = ConflatedBroadcastChannel<T>()
    public val localSetChannel: Channel<T> = Channel(capacity = Channel.CONFLATED)

    public override fun set(value: T) {
        localSetChannel.offer(value)
    }

    public override val valueOrNull: T?
        get() = broadcastChannel.valueOrNull

    public override fun openSubscription(): ReceiveChannel<T> = broadcastChannel.openSubscription()
}