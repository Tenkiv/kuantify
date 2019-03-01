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

package org.tenkiv.kuantify.fs.hardware.channel

import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.gate.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.inputs.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import tec.units.indriya.*
import javax.measure.quantity.*

private inline fun SideNetworkRouting<String>.startSamplingLocal(rc: String, crossinline start: () -> Unit) {
    bind<Ping>(rc, isFullyBiDirectional = false) {
        receive {
            start()
        }
    }
}

private fun SideNetworkRouting<String>.startSamplingRemote(rc: String, channel: ReceiveChannel<Ping>) {
    bind<Ping>(rc, isFullyBiDirectional = false) {
        setLocalUpdateChannel(channel) withUpdateChannel {
            send()
        }
    }
}

@Suppress("LeakingThis")
abstract class LocalDigitalInput : DigitalInput, NetworkBoundSide<String>, NetworkBoundCombined {

    abstract val uid: String

    private val thisAsBinaryStateSensor = SimpleBinaryStateSensor(this)

    private val thisAsTransitionFrequencyInput = SimpleDigitalFrequencySensor(this)

    private val thisAsPwmSensor = SimplePwmSensor(this)

    private val _isTransceiving: InitializedUpdatable<Boolean> = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    init {
        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    override fun asBinaryStateSensor(): BinaryStateInput = thisAsBinaryStateSensor

    override fun asTransitionFrequencySensor(avgFrequency: ComparableQuantity<Frequency>): QuantityInput<Frequency> {
        this.avgFrequency.set(avgFrequency)
        return thisAsTransitionFrequencyInput
    }

    override fun asPwmSensor(avgFrequency: ComparableQuantity<Frequency>): QuantityInput<Dimensionless> {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmSensor

    }

    override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            digitalGateRouting(this@LocalDigitalInput)
        }
    }

    override fun sideRouting(routing: SideNetworkRouting<String>) {
        routing.addToThisPath {
            digitalGateIsTransceivingLocal(isTransceivingBinaryState, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalGateIsTransceivingLocal(isTransceivingPwm, RC.IS_TRANSCEIVING_PWM)
            digitalGateIsTransceivingLocal(isTransceivingFrequency, RC.IS_TRANSCEIVING_FREQUENCY)

            startSamplingLocal(RC.START_SAMPLING_BINARY_STATE, ::startSamplingBinaryState)
            startSamplingLocal(RC.START_SAMPLING_PWM, ::startSamplingPwm)
            startSamplingLocal(RC.START_SAMPLING_TRANSITION_FREQUENCY, ::startSamplingTransitionFrequency)

            bind<ValueInstant<DigitalValue>>(RC.VALUE, isFullyBiDirectional = false) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(DigitalValue.serializer()), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}

@Suppress("LeakingThis")
abstract class FSRemoteDigitalInput : DigitalInput, NetworkBoundSide<String>, NetworkBoundCombined {
    abstract val uid: String

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<DigitalValue>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<DigitalValue>>
        get() = _updateBroadcaster

    private val _binaryStateBroadcaster = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    override val binaryStateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _binaryStateBroadcaster

    private val _pwmBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Dimensionless>>()
    override val pwmBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>
        get() = _pwmBroadcaster

    private val _transitionFrequencyBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Frequency>>()
    override val transitionFrequencyBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
        get() = _transitionFrequencyBroadcaster

    private val _isTransceiving = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _isTransceivingBinaryState = Updatable(false)
    override val isTransceivingBinaryState: InitializedTrackable<Boolean>
        get() = _isTransceivingBinaryState

    private val _isTransceivingPwm = Updatable(false)
    override val isTransceivingPwm: InitializedTrackable<Boolean>
        get() = _isTransceivingPwm

    private val _isTransceivingFrequency = Updatable(false)
    override val isTransceivingFrequency: InitializedTrackable<Boolean>
        get() = _isTransceivingFrequency

    private val thisAsBinaryStateSensor = SimpleBinaryStateSensor(this)

    private val thisAsTransitionFrequencyInput = SimpleDigitalFrequencySensor(this)

    private val thisAsPwmSensor = SimplePwmSensor(this)

    init {
        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    override val avgFrequency: UpdatableQuantity<Frequency> = Updatable()

    private val startSamplingTransitionFrequencyChannel = Channel<Ping>(Channel.CONFLATED)
    override fun startSamplingTransitionFrequency() {
        startSamplingTransitionFrequencyChannel.offer(null)
    }

    private val startSamplingPwmChannel = Channel<Ping>(Channel.CONFLATED)
    override fun startSamplingPwm() {
        startSamplingPwmChannel.offer(null)
    }

    private val startSamplingBinaryStateChannel = Channel<Ping>(Channel.CONFLATED)
    override fun startSamplingBinaryState() {
        startSamplingBinaryStateChannel.offer(null)
    }

    private val stopTransceivingChannel = Channel<Ping>(Channel.CONFLATED)
    override fun stopTransceiving() {
        stopTransceivingChannel.offer(null)
    }

    override fun asBinaryStateSensor(): BinaryStateInput = thisAsBinaryStateSensor

    override fun asTransitionFrequencySensor(avgFrequency: ComparableQuantity<Frequency>): QuantityInput<Frequency> {
        this.avgFrequency.set(avgFrequency)
        return thisAsTransitionFrequencyInput
    }

    override fun asPwmSensor(avgFrequency: ComparableQuantity<Frequency>): QuantityInput<Dimensionless> {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmSensor

    }

    override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            digitalGateRouting(this@FSRemoteDigitalInput)
        }
    }

    override fun sideRouting(routing: SideNetworkRouting<String>) {
        routing.addToThisPath {
            digitalGateIsTransceivingRemote(_isTransceivingBinaryState, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalGateIsTransceivingRemote(_isTransceivingPwm, RC.IS_TRANSCEIVING_PWM)
            digitalGateIsTransceivingRemote(_isTransceivingFrequency, RC.IS_TRANSCEIVING_FREQUENCY)

            startSamplingRemote(RC.START_SAMPLING_TRANSITION_FREQUENCY, startSamplingTransitionFrequencyChannel)
            startSamplingRemote(RC.START_SAMPLING_PWM, startSamplingPwmChannel)
            startSamplingRemote(RC.START_SAMPLING_BINARY_STATE, startSamplingBinaryStateChannel)

            bind<ValueInstant<DigitalValue>>(RC.VALUE, isFullyBiDirectional = false) {
                receive {
                    val measurement = Json.parse(ValueInstantSerializer(DigitalValue.serializer()), it)
                    val (value, instant) = measurement
                    when (value) {
                        is DigitalValue.BinaryState -> _binaryStateBroadcaster.send(value.state at instant)
                        is DigitalValue.Percentage -> _pwmBroadcaster.send(value.percent at instant)
                        is DigitalValue.Frequency ->
                            _transitionFrequencyBroadcaster.send(value.frequency at instant)
                    }
                    _updateBroadcaster.send(measurement)
                }
            }
        }
    }
}