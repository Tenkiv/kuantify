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
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.outputs.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import tec.units.indriya.*
import javax.measure.quantity.*

@Suppress("LeakingThis")
abstract class LocalDigitalOutput : DigitalOutput, NetworkBoundSide<String>, NetworkBoundCombined {

    abstract val uid: String

    private val thisAsBinaryStateController = SimpleBinaryStateController(this)

    private val thisAsPwmController = SimplePwmController(this)

    private val thisAsFrequencyController = SimpleFrequencyController(this)

    private val _isTransceiving: InitializedUpdatable<Boolean> = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<DigitalValue>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<DigitalValue>>
        get() = _updateBroadcaster

    init {
        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    override fun asBinaryStateController(): BinaryStateOutput = thisAsBinaryStateController

    override fun asPwmController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Dimensionless> {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmController
    }

    override fun asFrequencyController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Frequency> {
        this.avgFrequency.set(avgFrequency)
        return thisAsFrequencyController
    }

    override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            digitalGateRouting(this@LocalDigitalOutput)
        }
    }

    override fun sideRouting(routing: SideNetworkRouting<String>) {
        routing.addToThisPath {
            digitalGateIsTransceivingLocal(isTransceivingBinaryState, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalGateIsTransceivingLocal(isTransceivingPwm, RC.IS_TRANSCEIVING_PWM)
            digitalGateIsTransceivingLocal(isTransceivingFrequency, RC.IS_TRANSCEIVING_FREQUENCY)

            bind<ValueInstant<DigitalValue>>(RC.VALUE, isFullyBiDirectional = true) {
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
abstract class FSRemoteDigitalOutput : DigitalOutput, NetworkBoundSide<String>, NetworkBoundCombined {

    abstract val uid: String

    private val thisAsBinaryStateController = SimpleBinaryStateController(this)

    private val thisAsPwmController = SimplePwmController(this)

    private val thisAsFrequencyController = SimpleFrequencyController(this)

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

    init {

        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    override fun asBinaryStateController(): BinaryStateOutput = thisAsBinaryStateController

    override fun asPwmController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Dimensionless> {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmController
    }

    override fun asFrequencyController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Frequency> {
        this.avgFrequency.set(avgFrequency)
        return thisAsFrequencyController
    }

    override fun combinedRouting(routing: CombinedNetworkRouting) {
        routing.addToThisPath {
            digitalGateRouting(this@FSRemoteDigitalOutput)
        }
    }

    override fun sideRouting(routing: SideNetworkRouting<String>) {
        routing.addToThisPath {
            digitalGateIsTransceivingRemote(_isTransceivingBinaryState, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalGateIsTransceivingRemote(_isTransceivingPwm, RC.IS_TRANSCEIVING_PWM)
            digitalGateIsTransceivingRemote(_isTransceivingFrequency, RC.IS_TRANSCEIVING_FREQUENCY)

            bind<ValueInstant<DigitalValue>>(RC.VALUE, isFullyBiDirectional = true) {
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