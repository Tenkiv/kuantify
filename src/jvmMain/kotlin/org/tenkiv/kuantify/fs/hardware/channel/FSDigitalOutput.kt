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
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.device.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.networking.configuration.*
import physikal.*
import physikal.types.*

public abstract class LocalDigitalOutput<out DeviceT>(
    uid: String,
    public final override val device: DeviceT
) : LocalDigitalGate(uid), DigitalOutput where DeviceT : LocalDevice, DeviceT : DigitalOutputDevice {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(BinaryStateMeasurement.binaryStateSerializer(), RC.BIN_STATE_VALUE) {
                send(source = openBinaryStateSubscription())
            }
            bindFS(BinaryState.serializer(), RC.BIN_STATE_CONTROL_SETTING) {
                receive {
                    setOutputState(it)
                }
            }
            bindFS<QuantityMeasurement<Frequency>>(
                QuantityMeasurement.quantitySerializer(),
                RC.TRANSITION_FREQUENCY_VALUE
            ) {
                send(source = openTransitionFrequencySubscription())
            }
            bindFS<Quantity<Frequency>>(Quantity.serializer(), RC.TRANSITION_FREQUENCY_CONTROL_SETTING) {
                receive {
                    sustainTransitionFrequency(it)
                }
            }
            bindFS<QuantityMeasurement<Dimensionless>>(QuantityMeasurement.quantitySerializer(), RC.PWM_VALUE) {
                send(source = openPwmSubscription())
            }
            bindFS<Quantity<Dimensionless>>(Quantity.serializer(), RC.TRANSITION_FREQUENCY_CONTROL_SETTING) {
                receive {
                    pulseWidthModulate(it)
                }
            }
        }
    }
}

public abstract class FSRemoteDigitalOutput<out DeviceT>(
    uid: String,
    public final override val device: DeviceT
) : FSRemoteDigitalGate(uid, device), DigitalOutput where DeviceT : DigitalOutputDevice, DeviceT : FSRemoteDevice {

    @Volatile
    private var _lastStateSetting: BinaryStateMeasurement? = null
    public override val lastStateSetting: BinaryStateMeasurement?
        get() = _lastStateSetting

    @Volatile
    private var _lastPwmSetting: QuantityMeasurement<Dimensionless>? = null
    public override val lastPwmSetting: QuantityMeasurement<Dimensionless>?
        get() = _lastPwmSetting

    @Volatile
    private var _lastTransitionFrequencySetting: QuantityMeasurement<Frequency>? = null
    override val lastTransitionFrequencySetting: QuantityMeasurement<Frequency>?
        get() = _lastTransitionFrequencySetting

    private val binaryStateBroadcastChannel = BroadcastChannel<BinaryStateMeasurement>(capacity = Channel.BUFFERED)
    private val pwmBroadcastChannel = BroadcastChannel<QuantityMeasurement<Dimensionless>>(capacity = Channel.BUFFERED)
    private val transitionFrequencyBroadcastChannel =
        BroadcastChannel<QuantityMeasurement<Frequency>>(capacity = Channel.BUFFERED)

    public override fun openBinaryStateSubscription(): ReceiveChannel<BinaryStateMeasurement> =
        binaryStateBroadcastChannel.openSubscription()

    public override fun openPwmSubscription(): ReceiveChannel<QuantityMeasurement<Dimensionless>> =
        pwmBroadcastChannel.openSubscription()

    public override fun openTransitionFrequencySubscription(): ReceiveChannel<QuantityMeasurement<Frequency>> =
        transitionFrequencyBroadcastChannel.openSubscription()

    private suspend fun updateBinaryState(update: BinaryStateMeasurement) {
        _lastStateSetting = update
        binaryStateBroadcastChannel.send(update)
    }

    private suspend fun updatePwm(update: QuantityMeasurement<Dimensionless>) {
        _lastPwmSetting = update
        pwmBroadcastChannel.send(update)
    }

    private suspend fun updateTransitionFrequency(update: QuantityMeasurement<Frequency>) {
        _lastTransitionFrequencySetting = update
        transitionFrequencyBroadcastChannel.send(update)
    }

    private val binaryStateSettingChannel = Channel<BinaryState>(capacity = Channel.CONFLATED)
    public override fun setOutputState(state: BinaryState): SettingViability {
        command { binaryStateSettingChannel.offer(state) }
        return SettingViability.Viable
    }

    private val pwmSettingChannel = Channel<Quantity<Dimensionless>>(capacity = Channel.CONFLATED)
    public override fun pulseWidthModulate(percent: Quantity<Dimensionless>): SettingViability {
        command { pwmSettingChannel.offer(percent) }
        return SettingViability.Viable
    }

    private val transitionFrequencySettingChannel = Channel<Quantity<Frequency>>(capacity = Channel.CONFLATED)
    public override fun sustainTransitionFrequency(freq: Quantity<Frequency>): SettingViability {
        command { transitionFrequencySettingChannel.offer(freq) }
        return SettingViability.Viable
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
            bindFS<QuantityMeasurement<Dimensionless>>(
                QuantityMeasurement.quantitySerializer(),
                RC.TRANSITION_FREQUENCY_CONTROL_SETTING
            ) {
                receive {
                    updatePwm(it)
                }
            }
            bindFS(BinaryState.serializer(), RC.BIN_STATE_CONTROL_SETTING) {
                send(source = binaryStateSettingChannel)
            }
            bindFS<Quantity<Frequency>>(Quantity.serializer(), RC.TRANSITION_FREQUENCY_VALUE) {
                send(source = transitionFrequencySettingChannel)
            }
            bindFS<Quantity<Dimensionless>>(Quantity.serializer(), RC.PWM_CONTROL_SETTING) {
                send(source = pwmSettingChannel)
            }
        }
    }

}