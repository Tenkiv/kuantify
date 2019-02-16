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

package org.tenkiv.kuantify.gate.acquire

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import kotlin.coroutines.*

abstract class FSRemoteAcquireGate<T : DaqcData>(private val scope: CoroutineScope) : AcquireGate<T>,
    NetworkConfiguredSide {

    abstract val uid: String

    final override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    //TODO
    internal val _failureBroadcaster = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    override val failureBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcaster

    internal val startSamplingChannel = Channel<Ping>(Channel.CONFLATED)
    override fun startSampling() {
        startSamplingChannel.offer(null)
    }

    internal val stopTransceivingChannel = Channel<Ping>(Channel.CONFLATED)
    override fun stopTransceiving() {
        stopTransceivingChannel.offer(null)
    }

    override fun sideConfig(config: SideRouteConfig) {
        val inputRoute = listOf(RC.DAQC_GATE, uid)

        config.add {
            route(inputRoute + RC.START_SAMPLING) to handler<Ping>(isFullyBiDirectional = false) {
                setLocalUpdateChannel(startSamplingChannel) withUpdateChannel {
                    send()
                }
            }

            route(inputRoute + RC.STOP_TRANSCEIVING) to handler<Ping>(isFullyBiDirectional = false) {
                setLocalUpdateChannel(stopTransceivingChannel) withUpdateChannel {
                    send()
                }
            }
        }

    }

}