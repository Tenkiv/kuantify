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

package kuantify.fs.gate

import kotlinx.coroutines.channels.*
import kotlinx.serialization.builtins.*
import kuantify.*
import kuantify.fs.hardware.device.*
import kuantify.fs.networking.*
import kuantify.gate.*
import kuantify.hardware.channel.*
import kuantify.networking.configuration.*
import kuantify.trackable.*
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
        //TODO: Should be atomic compare and set
        if (!isFinalized) {
            _isFinalized.set(true)
            finalizeChannel.offer(Ping)
        }
    }

    public override fun routing(route: NetworkRoute<String>): Unit =
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

            bindFS(Boolean.serializer(), RC.IS_TRANSCEIVING) {
                send(source = isTransceiving.openSubscription())
            }
        }

}

public abstract class FSRemoteDaqcGate(
    public final override val uid: String
) : DaqcGate, RemoteDeviceGate, NetworkBound<String> {
    public abstract override val device: FSRemoteDevice
    public final override val basePath: Path = listOf(RC.DAQC_GATE, uid)

    private val _isTransceiving = Updatable<Boolean>()
    public final override val isTransceiving: Trackable<Boolean>
        get() = _isTransceiving

    // stop transceiving
    private val stopTransceivingChannel = Channel<Ping>(Channel.RENDEZVOUS)
    public final override fun stopTransceiving(): Unit =
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

    public override fun routing(route: NetworkRoute<String>): Unit =
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

            bindFS(Boolean.serializer(), RC.IS_TRANSCEIVING) {
                receive(networkChannelCapacity = Channel.CONFLATED) { value ->
                    modifyConfiguration {
                        _isTransceiving.set(value)
                    }
                }
            }
        }

}