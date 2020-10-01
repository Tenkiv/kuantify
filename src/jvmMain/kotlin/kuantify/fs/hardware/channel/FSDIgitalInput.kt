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

package kuantify.fs.hardware.channel

import kotlinx.coroutines.channels.*
import kuantify.fs.gate.*
import kuantify.fs.hardware.device.*
import kuantify.fs.networking.*
import kuantify.hardware.channel.*
import kuantify.hardware.device.*
import kuantify.lib.*
import kuantify.lib.physikal.*
import kuantify.networking.configuration.*
import physikal.types.*


public abstract class LocalDigitalInput<out DeviceT>(
    uid: String,
    public final override val device: DeviceT
) : LocalDigitalGate(uid), DigitalInput where DeviceT : LocalDevice, DeviceT : DigitalDaqDevice {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(BinaryStateMeasurement.binaryStateSerializer(), RC.BIN_STATE_VALUE) {
                send(source = openBinaryStateSubscription())
            }
            bindFS<QuantityMeasurement<Frequency>>(
                QuantityMeasurement.quantitySerializer(),
                RC.TRANSITION_FREQUENCY_VALUE
            ) {
                send(source = openTransitionFrequencySubscription())
            }
            bindFS<QuantityMeasurement<Dimensionless>>(QuantityMeasurement.quantitySerializer(), RC.PWM_VALUE) {
                send(source = openPwmSubscription())
            }
            bindPing(RC.START_SAMPLING_BINARY_STATE) {
                receive {
                    startSamplingBinaryState()
                }
            }
            bindPing(RC.START_SAMPLING_TRANSITION_FREQUENCY) {
                receive {
                    startSamplingTransitionFrequency()
                }
            }
            bindPing(RC.START_SAMPLING_PWM) {
                receive {
                    startSamplingPwm()
                }
            }
        }
    }

}

public abstract class FSRemoteDigitalInput<out DeviceT>(
    uid: String,
    public final override val device: DeviceT
) : FSRemoteDigitalGate(uid, device), DigitalInput where DeviceT : DigitalDaqDevice, DeviceT : FSRemoteDevice {

    @Volatile
    private var _lastStateMeasurement: BinaryStateMeasurement? = null
    public override val lastStateMeasurement: BinaryStateMeasurement?
        get() = _lastStateMeasurement

    @Volatile
    private var _lastPwmMeasurement: QuantityMeasurement<Dimensionless>? = null
    public override val lastPwmMeasurement: QuantityMeasurement<Dimensionless>?
        get() = _lastPwmMeasurement

    @Volatile
    private var _lastTransitionFrequencyMeasuremnt: QuantityMeasurement<Frequency>? = null
    override val lastTransitionFrequencyMeasurement: QuantityMeasurement<Frequency>?
        get() = _lastTransitionFrequencyMeasuremnt

    private val binaryStateBroadcastChannel = BroadcastChannel<BinaryStateMeasurement>(capacity = Channel.BUFFERED)
    private val pwmBroadcastChannel = BroadcastChannel<QuantityMeasurement<Dimensionless>>(capacity = Channel.BUFFERED)
    private val transitionFrequencyBroadcastChannel = BroadcastChannel<QuantityMeasurement<Frequency>>(capacity = Channel.BUFFERED)

    public override fun openBinaryStateSubscription(): ReceiveChannel<BinaryStateMeasurement> =
        binaryStateBroadcastChannel.openSubscription()

    public override fun openPwmSubscription(): ReceiveChannel<QuantityMeasurement<Dimensionless>> =
        pwmBroadcastChannel.openSubscription()

    public override fun openTransitionFrequencySubscription(): ReceiveChannel<QuantityMeasurement<Frequency>> =
        transitionFrequencyBroadcastChannel.openSubscription()

    private suspend fun updateBinaryState(update: BinaryStateMeasurement) {
        _lastStateMeasurement = update
        binaryStateBroadcastChannel.send(update)
    }

    private suspend fun updatePwm(update: QuantityMeasurement<Dimensionless>) {
        _lastPwmMeasurement = update
        pwmBroadcastChannel.send(update)
    }

    private suspend fun updateTransitionFrequency(update: QuantityMeasurement<Frequency>) {
        _lastTransitionFrequencyMeasuremnt = update
        transitionFrequencyBroadcastChannel.send(update)
    }

    private val startSamplingBinaryStateChannel = Channel<Ping>(capacity = Channel.RENDEZVOUS)
    public override fun startSamplingBinaryState() {
        startSamplingBinaryStateChannel.offer(Ping)
    }

    private val startSamplingTransitionFrequencyChannel = Channel<Ping>(capacity = Channel.RENDEZVOUS)
    public override fun startSamplingTransitionFrequency() {
        startSamplingTransitionFrequencyChannel.offer(Ping)
    }

    private val startSamplingPwmChannel = Channel<Ping>(capacity = Channel.RENDEZVOUS)
    public override fun startSamplingPwm() {
        startSamplingPwmChannel.offer(Ping)
    }

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(BinaryStateMeasurement.binaryStateSerializer(), RC.BIN_STATE_VALUE) {
                receive {
                    updateBinaryState(it)
                }
            }
            bindFS<QuantityMeasurement<Frequency>>(
                QuantityMeasurement.quantitySerializer(),
                RC.TRANSITION_FREQUENCY_VALUE
            ) {
                receive {
                    updateTransitionFrequency(it)
                }
            }
            bindFS<QuantityMeasurement<Dimensionless>>(QuantityMeasurement.quantitySerializer(), RC.PWM_VALUE) {
                receive {
                    updatePwm(it)
                }
            }
            bindPing(RC.START_SAMPLING_BINARY_STATE) {
                send(source = startSamplingBinaryStateChannel)
            }
            bindPing(RC.START_SAMPLING_TRANSITION_FREQUENCY) {
                send(source = startSamplingTransitionFrequencyChannel)
            }
            bindPing(RC.START_SAMPLING_PWM) {
                send(source = startSamplingPwmChannel)
            }
        }
    }

}