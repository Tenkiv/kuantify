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

package org.tenkiv.kuantify.fs.hardware.channel

import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.gate.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.networking.communication.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.kuantify.trackable.*
import physikal.*
import physikal.types.*


public abstract class LocalDigitalInput<out DeviceT>(
    uid: String,
    public final override val device: DeviceT
) : LocalDigitalGate(uid), DigitalInput
        where DeviceT : LocalDevice, DeviceT : DigitalDaqDevice {

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
    private val binaryStateBroadcaster = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    private val pwmBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Dimensionless>>()
    private val transitionFrequencyBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Frequency>>()

    private val _isTransceivingBinaryState = Updatable<Boolean>()
    public override val isTransceivingBinaryState: Trackable<Boolean>
        get() = _isTransceivingBinaryState

    private val _isTransceivingPwm = Updatable<Boolean>()
    public override val isTransceivingPwm: Trackable<Boolean>
        get() = _isTransceivingPwm

    private val _isTransceivingFrequency = Updatable<Boolean>()
    public override val isTransceivingFrequency: Trackable<Boolean>
        get() = _isTransceivingFrequency

    private val startSamplingTransitionFrequencyChannel = Channel<Ping>(Channel.RENDEZVOUS)
    public override fun startSamplingTransitionFrequency() {
        startSamplingTransitionFrequencyChannel.offer(Ping)
    }

    private val startSamplingPwmChannel = Channel<Ping>(Channel.RENDEZVOUS)
    public override fun startSamplingPwm() {
        startSamplingPwmChannel.offer(Ping)
    }

    private val startSamplingBinaryStateChannel = Channel<Ping>(Channel.RENDEZVOUS)
    public override fun startSamplingBinaryState() {
        startSamplingBinaryStateChannel.offer(Ping)
    }

    private val stopTransceivingChannel = Channel<Ping>(Channel.RENDEZVOUS)
    public override fun stopTransceiving() {
        stopTransceivingChannel.offer(Ping)
    }

    public override fun asBinaryStateSensor(): BinaryStateInput = thisAsBinaryStateSensor

    public override fun asTransitionFrequencySensor(avgFrequency: Quantity<Frequency>):
            QuantityInput<Frequency> {
        this.avgPeriod.set(avgFrequency)
        return thisAsTransitionFrequencyInput
    }

    public override fun asPwmSensor(avgFrequency: Quantity<Frequency>): QuantityInput<Dimensionless> {
        this.avgPeriod.set(avgFrequency)
        return thisAsPwmSensor

    }

    public override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            digitalGateRouting(this@FSRemoteDigitalInput)
        }
    }

    public override fun routing(route: NetworkRoute<String>) {
        route.add {
            digitalGateIsTransceivingRemote(_isTransceivingBinaryState, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalGateIsTransceivingRemote(_isTransceivingPwm, RC.IS_TRANSCEIVING_PWM)
            digitalGateIsTransceivingRemote(_isTransceivingFrequency, RC.IS_TRANSCEIVING_FREQUENCY)

            startSamplingRemote(RC.START_SAMPLING_TRANSITION_FREQUENCY, startSamplingTransitionFrequencyChannel)
            startSamplingRemote(RC.START_SAMPLING_PWM, startSamplingPwmChannel)
            startSamplingRemote(RC.START_SAMPLING_BINARY_STATE, startSamplingBinaryStateChannel)

            bind<ValueInstant<DigitalValue>>(RC.VALUE) {
                receive {
                    val measurement = Serialization.json.parse(ValueInstantSerializer(DigitalValue.serializer()), it)
                    val (value, instant) = measurement
                    when (value) {
                        is DigitalValue.BinaryState -> binaryStateBroadcaster.send(value.state at instant)
                        is DigitalValue.Percentage -> pwmBroadcaster.send(value.percent at instant)
                        is DigitalValue.Frequency ->
                            transitionFrequencyBroadcaster.send(value.frequency at instant)
                    }
                    _updateBroadcaster.send(measurement)
                }
            }
        }
    }

}