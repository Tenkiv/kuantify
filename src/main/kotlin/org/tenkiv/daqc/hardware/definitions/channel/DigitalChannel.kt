package org.tenkiv.daqc.hardware.definitions.channel

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.daqc.BinaryStateMeasurement
import org.tenkiv.daqc.Measurement
import org.tenkiv.daqc.QuantityMeasurement
import org.tenkiv.daqc.hardware.definitions.DaqcChannel
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.lib.openNewCoroutineListener
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

/**
 * Copyright 2017 TENKIV, INC.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
abstract class DigitalChannel : Updatable<BinaryStateMeasurement>, DaqcChannel {

    open val isActive get() = isActiveForBinaryState || isActiveForPwm || isActiveForTransitionFrequency

    /**
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract val isActiveForBinaryState: Boolean

    /**
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract val isActiveForPwm: Boolean

    /**
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract val isActiveForTransitionFrequency: Boolean

    protected val _binaryStateBroadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    final override val broadcastChannel: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _binaryStateBroadcastChannel

    protected val _pwmBroadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Dimensionless>>()

    val pwmBroadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>
        get() = _pwmBroadcastChannel

    protected val _transitionFrequencyBroadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Frequency>>()

    val transitionFrequencyBroadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
        get() = _transitionFrequencyBroadcastChannel

    private val _unifiedBroadcastChannel = ConflatedBroadcastChannel<Measurement>()

    val unifiedBroadcastChannel: ConflatedBroadcastChannel<out Measurement>
        get() = _unifiedBroadcastChannel

    abstract val pwmIsSimulated: Boolean

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