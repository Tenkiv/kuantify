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
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.fs.gate.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.configuration.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.hardware.outputs.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.kuantify.trackable.*
import physikal.*
import physikal.types.*

@Suppress("LeakingThis")
public abstract class LocalDigitalOutput<out D> : DigitalOutput<D>, NetworkBoundSide<String>, NetworkBoundCombined
        where D : LocalDevice, D : DigitalOutputDevice {

    private val thisAsBinaryStateController = SimpleBinaryStateController(this)

    private val thisAsPwmController = SimplePwmController(this)

    private val thisAsFrequencyController = SimpleFrequencyController(this)

    private val _isTransceiving: InitializedUpdatable<Boolean> = Updatable(false)
    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<DigitalValue>>()
    public override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<DigitalValue>>
        get() = _updateBroadcaster

    init {
        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    public override fun asBinaryStateController(): BinaryStateOutput = thisAsBinaryStateController

    public override fun asPwmController(avgFrequency: Quantity<Frequency>): QuantityOutput<Dimensionless> {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmController
    }

    public override fun asFrequencyController(avgFrequency: Quantity<Frequency>): QuantityOutput<Frequency> {
        this.avgFrequency.set(avgFrequency)
        return thisAsFrequencyController
    }

    public override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            digitalGateRouting(this@LocalDigitalOutput)
        }
    }

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        routing.addToThisPath {
            digitalGateIsTransceivingLocal(isTransceivingBinaryState, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalGateIsTransceivingLocal(isTransceivingPwm, RC.IS_TRANSCEIVING_PWM)
            digitalGateIsTransceivingLocal(isTransceivingFrequency, RC.IS_TRANSCEIVING_FREQUENCY)

            bind<ValueInstant<DigitalValue>>(RC.VALUE) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(DigitalValue.serializer()), it)
                }

                receive {
                    val setting = Json.parse(ValueInstantSerializer(DigitalValue.serializer()), it)
                    val (value, _) = setting
                    when (value) {
                        is DigitalValue.BinaryState -> setOutputState(value.state)
                        is DigitalValue.Percentage -> pulseWidthModulate(value.percent)
                        is DigitalValue.Frequency -> sustainTransitionFrequency(value.frequency)
                    }
                    _updateBroadcaster.send(setting)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}

@Suppress("LeakingThis")
public abstract class FSRemoteDigitalOutput<out D> : DigitalOutput<D>, NetworkBoundSide<String>, NetworkBoundCombined
        where D : DigitalOutputDevice, D : FSRemoteDevice {

    private val thisAsBinaryStateController = SimpleBinaryStateController(this)

    private val thisAsPwmController = SimplePwmController(this)

    private val thisAsFrequencyController = SimpleFrequencyController(this)

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<DigitalValue>>()
    public override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<DigitalValue>>
        get() = _updateBroadcaster

    private val _binaryStateBroadcaster = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    public override val binaryStateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _binaryStateBroadcaster

    private val _pwmBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Dimensionless>>()
    override val pwmBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>
        get() = _pwmBroadcaster

    private val _transitionFrequencyBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Frequency>>()
    public override val transitionFrequencyBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
        get() = _transitionFrequencyBroadcaster

    private val _isTransceiving = Updatable(false)
    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _isTransceivingBinaryState = Updatable(false)
    public override val isTransceivingBinaryState: InitializedTrackable<Boolean>
        get() = _isTransceivingBinaryState

    private val _isTransceivingPwm = Updatable(false)
    public override val isTransceivingPwm: InitializedTrackable<Boolean>
        get() = _isTransceivingPwm

    private val _isTransceivingFrequency = Updatable(false)
    public override val isTransceivingFrequency: InitializedTrackable<Boolean>
        get() = _isTransceivingFrequency

    init {

        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    public override fun asBinaryStateController(): BinaryStateOutput = thisAsBinaryStateController

    public override fun asPwmController(avgFrequency: Quantity<Frequency>): QuantityOutput<Dimensionless> {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmController
    }

    public override fun asFrequencyController(avgFrequency: Quantity<Frequency>): QuantityOutput<Frequency> {
        this.avgFrequency.set(avgFrequency)
        return thisAsFrequencyController
    }

    public override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            digitalGateRouting(this@FSRemoteDigitalOutput)
        }
    }

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        routing.addToThisPath {
            digitalGateIsTransceivingRemote(_isTransceivingBinaryState, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalGateIsTransceivingRemote(_isTransceivingPwm, RC.IS_TRANSCEIVING_PWM)
            digitalGateIsTransceivingRemote(_isTransceivingFrequency, RC.IS_TRANSCEIVING_FREQUENCY)

            bind<ValueInstant<DigitalValue>>(RC.VALUE) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(DigitalValue.serializer()), it)
                }

                receive {
                    val setting = Json.parse(ValueInstantSerializer(DigitalValue.serializer()), it)
                    val (value, instant) = setting
                    when (value) {
                        is DigitalValue.BinaryState -> _binaryStateBroadcaster.send(value.state at instant)
                        is DigitalValue.Percentage -> _pwmBroadcaster.send(value.percent at instant)
                        is DigitalValue.Frequency ->
                            _transitionFrequencyBroadcaster.send(value.frequency at instant)
                    }
                    _updateBroadcaster.send(setting)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}