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

package org.tenkiv.kuantify.fs.gate.acquire

import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.acquire.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.networking.configuration.*
import kotlin.coroutines.*

abstract class FSRemoteAcquireGate<T : DaqcData, out D : FSRemoteDevice>(
    final override val device: D,
    final override val uid: String
) : AcquireGate<T>, DeviceGate<T, D>, NetworkBoundSide<String> {

    final override val coroutineContext: CoroutineContext
        get() = device.coroutineContext

    final override val basePath: Path = listOf(RC.DAQC_GATE, uid)

    internal val startSamplingChannel = Channel<Ping>(Channel.CONFLATED)
    final override fun startSampling() {
        command {
            startSamplingChannel.offer(Ping)
        }
    }

    internal val stopTransceivingChannel = Channel<Ping>(Channel.CONFLATED)
    final override fun stopTransceiving() {
        command {
            stopTransceivingChannel.offer(Ping)
        }
    }

    override fun sideRouting(routing: SideNetworkRouting<String>) {
        routing.addToThisPath {

            bind<Ping>(RC.START_SAMPLING) {
                setLocalUpdateChannel(startSamplingChannel) withUpdateChannel {
                    send()
                }
            }

            bind<Ping>(RC.STOP_TRANSCEIVING) {
                setLocalUpdateChannel(stopTransceivingChannel) withUpdateChannel {
                    send()
                }
            }

        }

    }

}