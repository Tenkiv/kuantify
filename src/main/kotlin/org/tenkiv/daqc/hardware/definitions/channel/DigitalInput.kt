package org.tenkiv.daqc.hardware.definitions.channel

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.BinaryStateMeasurement
import org.tenkiv.Measurement
import org.tenkiv.QuantityMeasurement
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.DigitalDaqcChannel
import org.tenkiv.daqc.lib.openNewCoroutineListener
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

abstract class DigitalInput : Input<BinaryState>, DigitalDaqcChannel {

    protected val _binaryStateBroadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    final override val broadcastChannel get() = _binaryStateBroadcastChannel

    protected val _transitionFrequencyBroadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Frequency>>()

    val transitionFrequencyBroadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
        get() = _transitionFrequencyBroadcastChannel

    protected val _pwmBroadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Dimensionless>>()

    val pwmBroadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>
        get() = _pwmBroadcastChannel

    private val _unifiedBroadcastChannel = ConflatedBroadcastChannel<Measurement>()

    val unifiedBroadcastChannel: ConflatedBroadcastChannel<out Measurement>
        get() = _unifiedBroadcastChannel

    override val isActive get() = super.isActive

    init {
        broadcastChannel.openNewCoroutineListener(CommonPool) {
            _unifiedBroadcastChannel.send(it)
        }
        transitionFrequencyBroadcastChannel.openNewCoroutineListener(CommonPool) {
            _unifiedBroadcastChannel.send(it)
        }
        pwmBroadcastChannel.openNewCoroutineListener(CommonPool) {
            _unifiedBroadcastChannel.send(it)
        }
    }

    abstract fun activateForTransitionFrequency()

    abstract fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>)

    open fun activateForCurrentState() = activate()
}