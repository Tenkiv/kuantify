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
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

abstract class ScPwmSensor<Q : Quantity<Q>>(
    val digitalInput: DigitalInput,
    val avgFrequency: DaqcQuantity<Frequency>
) : QuantityInput<Q> {

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()
    final override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    final override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForPwm

    init {
        digitalInput.pwmBroadcastChannel.openNewCoroutineListener(CommonPool) { measurement ->
            val convertedInput = convertInput(measurement.value)

            when (convertedInput) {
                is Success -> _broadcastChannel.send(convertedInput.value at measurement.instant)
                is Failure -> _failureBroadcastChannel.send(convertedInput.exception at measurement.instant)
            }
        }
    }

    override fun activate() = digitalInput.activateForPwm(avgFrequency)

    override fun deactivate() = digitalInput.deactivate()

    protected abstract fun convertInput(percentOn: ComparableQuantity<Dimensionless>): Try<DaqcQuantity<Q>>
}