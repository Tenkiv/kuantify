package org.tenkiv.daqc.hardware.inputs

import arrow.core.Failure
import arrow.core.Success
import arrow.core.Try
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.QuantityInput
import org.tenkiv.daqc.QuantityMeasurement
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.lib.openNewCoroutineListener
import org.tenkiv.daqc.toDaqc
import org.tenkiv.physikal.core.hertz
import tec.units.indriya.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Frequency


abstract class ScDigitalFrequencySensor<Q : Quantity<Q>>(val digitalInput: DigitalInput) :
    QuantityInput<Q> {

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()
    final override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    final override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForTransitionFrequency

    override val sampleRate get() = digitalInput.sampleRate

    init {
        digitalInput.transitionFrequencyBroadcastChannel.openNewCoroutineListener(CommonPool) { measurement ->
            val convertedInput = convertInput(measurement.value)

            when (convertedInput) {
                is Success -> _broadcastChannel.send(convertedInput.value at measurement.instant)
                is Failure -> _failureBroadcastChannel.send(convertedInput.exception at measurement.instant)
            }
        }
    }

    @Volatile
    var averageTransitionFrequency: DaqcQuantity<Frequency> = 2.hertz.toDaqc()

    override fun activate() = digitalInput.activateForTransitionFrequency(averageTransitionFrequency)

    override fun deactivate() = digitalInput.deactivate()

    protected abstract fun convertInput(frequency: ComparableQuantity<Frequency>): Try<DaqcQuantity<Q>>
}