/*
 * Copyright 2018 Tenkiv, Inc.
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

package org.tenkiv.daqc.hardware.definitions.channel

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.daqc.BinaryStateMeasurement
import org.tenkiv.daqc.Measurement
import org.tenkiv.daqc.QuantityMeasurement
import org.tenkiv.daqc.Updatable
import org.tenkiv.daqc.lib.openNewCoroutineListener
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

/**
 * Class defining the basic aspects that define both [DigitalOutput]s, [DigitalInput]s, and other digital channels.
 */
abstract class DigitalChannel : Updatable<BinaryStateMeasurement>, DaqcChannel {

    /**
     * Gets if the channel has been activated for any state
     */
    open val isActive get() = isActiveForBinaryState || isActiveForPwm || isActiveForTransitionFrequency

    /**
     * Gets if the channel is active for binary state.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract val isActiveForBinaryState: Boolean

    /**
     * Gets if the channel is active for pulse width modulation.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract val isActiveForPwm: Boolean

    /**
     * Gets if the channel is active for state transitions.
     *
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract val isActiveForTransitionFrequency: Boolean

    protected val _binaryStateBroadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    final override val broadcastChannel: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _binaryStateBroadcastChannel

    protected val _pwmBroadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Dimensionless>>()

    /**
     * [ConflatedBroadcastChannel] for receiving pulse width modulation data.
     */
    val pwmBroadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>
        get() = _pwmBroadcastChannel

    protected val _transitionFrequencyBroadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Frequency>>()

    /**
     * [ConflatedBroadcastChannel] for receiving transition frequency data.
     */
    val transitionFrequencyBroadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
        get() = _transitionFrequencyBroadcastChannel

    private val _unifiedBroadcastChannel = ConflatedBroadcastChannel<Measurement>()

    /**
     * [ConflatedBroadcastChannel] for receiving all data sent by this channel.
     */
    val unifiedBroadcastChannel: ConflatedBroadcastChannel<out Measurement>
        get() = _unifiedBroadcastChannel

    /**
     * Gets if the pulse width modulation state for this channel is simulated using software.
     */
    abstract val pwmIsSimulated: Boolean

    /**
     * Gets if the transition frequency state for this channel is simulated using software.
     */
    abstract val transitionFrequencyIsSimulated: Boolean

    init {
        broadcastChannel.openNewCoroutineListener(CommonPool) {
            _unifiedBroadcastChannel.send(it)
        }
        pwmBroadcastChannel.openNewCoroutineListener(CommonPool) {
            _unifiedBroadcastChannel.send(it)
        }
        transitionFrequencyBroadcastChannel.openNewCoroutineListener(CommonPool) {
            _unifiedBroadcastChannel.send(it)
        }
    }
}