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
import kotlinx.coroutines.flow.*
import kuantify.data.*
import kuantify.fs.gate.*
import kuantify.fs.hardware.device.*
import kuantify.fs.networking.*
import kuantify.gate.control.*
import kuantify.hardware.channel.*
import kuantify.hardware.device.*
import kuantify.lib.*
import kuantify.lib.physikal.*
import kuantify.networking.configuration.*
import org.tenkiv.coral.*
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
                send(source = binaryStateFlow)
            }
            bindFS(BinaryState.serializer(), RC.BIN_STATE_CONTROL_SETTING) {
                receive {
                    setOutputStateIV(it)
                }
            }
            bindFS<QuantityMeasurement<Frequency>>(
                QuantityMeasurement.quantitySerializer(),
                RC.TRANSITION_FREQUENCY_VALUE
            ) {
                send(source = transitionFrequencyFlow)
            }
            bindFS<Quantity<Frequency>>(Quantity.serializer(), RC.TRANSITION_FREQUENCY_CONTROL_SETTING) {
                receive {
                    sustainTransitionFrequencyIV(it)
                }
            }
            bindFS<QuantityMeasurement<Dimensionless>>(QuantityMeasurement.quantitySerializer(), RC.PWM_VALUE) {
                send(source = pwmFlow)
            }
            bindFS<Quantity<Dimensionless>>(Quantity.serializer(), RC.TRANSITION_FREQUENCY_CONTROL_SETTING) {
                receive {
                    pulseWidthModulateIV(it)
                }
            }
        }
    }

}

public abstract class FSRemoteDigitalOutput<out DeviceT>(
    uid: String,
    public final override val device: DeviceT,
    binaryStateBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER,
    pwmBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER / 8u,
    tfBufferCapacity: UInt32 = RC.DEFAULT_HIGH_LOAD_BUFFER / 8u
) : FSRemoteDigitalGate(uid, device), DigitalOutput where DeviceT : DigitalOutputDevice, DeviceT : FSRemoteDevice {

    private val _binaryStateFlow: MutableSharedFlow<BinaryStateInstant> = MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = binaryStateBufferCapacity.toInt32(),
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    override val binaryStateFlow: SharedFlow<BinaryStateInstant> get() = _binaryStateFlow

    private val _pwmFlow: MutableSharedFlow<QuantityInstant<Dimensionless>> = MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = pwmBufferCapacity.toInt32(),
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    override val pwmFlow: SharedFlow<QuantityInstant<Dimensionless>> get() = _pwmFlow

    private val _transitionFrequencyFlow: MutableSharedFlow<QuantityInstant<Frequency>> = MutableSharedFlow(
        replay = 1,
        extraBufferCapacity = tfBufferCapacity.toInt32(),
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    override val transitionFrequencyFlow: SharedFlow<QuantityInstant<Frequency>>
        get() = _transitionFrequencyFlow

    private suspend fun updateBinaryState(update: BinaryStateInstant) {
        _binaryStateFlow.emit(update)
    }

    private suspend fun updatePwm(update: QuantityInstant<Dimensionless>) {
        _pwmFlow.emit(update)
    }

    private suspend fun updateTransitionFrequency(update: QuantityInstant<Frequency>) {
        _transitionFrequencyFlow.emit(update)
    }

    private val binaryStateSettingChannel = Channel<BinaryState>(
        capacity = binaryStateBufferCapacity.toInt32()
    )
    public override suspend fun setOutputStateIV(state: BinaryState): SettingViability {
        command { binaryStateSettingChannel.send(state) }
        return SettingViability.Viable
    }

    private val pwmSettingChannel = Channel<Quantity<Dimensionless>>(
        capacity = pwmBufferCapacity.toInt32()
    )

    public override suspend fun pulseWidthModulateIV(percent: Quantity<Dimensionless>): SettingViability {
        command { pwmSettingChannel.send(percent) }
        return SettingViability.Viable
    }

    private val transitionFrequencySettingChannel = Channel<Quantity<Frequency>>(
        capacity = tfBufferCapacity.toInt32()
    )

    public override suspend fun sustainTransitionFrequencyIV(freq: Quantity<Frequency>): SettingViability {
        command { transitionFrequencySettingChannel.send(freq) }
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