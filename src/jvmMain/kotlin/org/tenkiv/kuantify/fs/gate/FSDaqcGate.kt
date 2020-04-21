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

package org.tenkiv.kuantify.fs.gate

import kotlinx.coroutines.channels.*
import kotlinx.serialization.builtins.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.kuantify.trackable.*
import java.util.concurrent.atomic.*

public abstract class LocalDaqcGate(
    public final override val uid: String
) : DaqcGate, DeviceGate, NetworkBound<String> {
    public abstract override val device: LocalDevice
    public final override val basePath: Path = listOf(RC.DAQC_GATE, uid)

    // finalize
    private val _isFinalized = AtomicBoolean(false)
    public override val isFinalized: Boolean
        get() = _isFinalized.get()
    private val finalizeChannel = Channel<Ping>(capacity = Channel.RENDEZVOUS)
    public override fun finalize() {
        if (!isFinalized) {
            _isFinalized.set(true)
            finalizeChannel.offer(Ping)
        }
    }

    public override fun routing(route: NetworkRoute<String>) =
        route.add {
            bindPing(RC.FINALIZE) {
                send(source = finalizeChannel)
                receive {
                    _isFinalized.set(true)
                }
            }

            bindPing(RC.STOP_TRANSCEIVING) {
                receive {
                    stopTransceiving()
                }
            }

            bind<Boolean>(RC.IS_TRANSCEIVING) {
                send(source = isTransceiving.openSubscription()) {
                    Serialization.json.stringify(Boolean.serializer(), it)
                }
            }
        }

}

public abstract class FSRemoteDaqcGate(
    public final override val uid: String
) : DaqcGate, RemoteDeviceGate, NetworkBound<String> {
    public abstract override val device: FSRemoteDevice
    public final override val basePath: Path = listOf(RC.DAQC_GATE, uid)

    private val _isTransceiving = Updatable(false)
    public final override val isTransceiving: Trackable<Boolean>
        get() = _isTransceiving

    // stop transceiving
    private val stopTransceivingChannel = Channel<Ping>(Channel.RENDEZVOUS)
    public final override fun stopTransceiving() =
        modifyConfiguration {
            command {
                stopTransceivingChannel.offer(Ping)
            }
            return@modifyConfiguration
        }

    // finalize
    private val _isFinalized = AtomicBoolean(false)
    public override val isFinalized: Boolean
        get() = _isFinalized.get()
    private val finalizeChannel = Channel<Ping>(Channel.RENDEZVOUS)
    // Not a command, it's ok if there is no connection when finalize is called.
    public final override fun finalize() {
        if (!isFinalized) {
            _isFinalized.set(true)
            finalizeChannel.offer(Ping)
        }
    }

    public override fun routing(route: NetworkRoute<String>) =
        route.add {
            bindPing(RC.FINALIZE) {
                send(source = finalizeChannel)
                receive {
                    _isFinalized.set(true)
                }
            }

            bindPing(RC.STOP_TRANSCEIVING) {
                send(source = stopTransceivingChannel)
            }

            bind<Boolean>(RC.IS_TRANSCEIVING) {
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    modifyConfiguration {
                        val value = Serialization.json.parse(Boolean.serializer(), it)
                        _isTransceiving.value = value
                    }
                }
            }
        }

}