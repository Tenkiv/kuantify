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
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential


abstract class ScAnalogSensor<Q : Quantity<Q>>(
    val analogInput: AnalogInput,
    maximumEp: ComparableQuantity<ElectricPotential>,
    acceptableError: ComparableQuantity<ElectricPotential>
) : QuantityInput<Q> {

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()
    final override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    final override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    override val isActive get() = analogInput.isActive

    override val sampleRate get() = analogInput.sampleRate

    init {
        analogInput.maxElectricPotential = maximumEp
        analogInput.maxAcceptableError = acceptableError
        analogInput.openNewCoroutineListener(CommonPool) { measurement ->

            val convertedResult = convertInput(measurement.value)

            when (convertedResult) {
                is Success -> _broadcastChannel.send(convertedResult.value at measurement.instant)
                is Failure -> _failureBroadcastChannel.send(convertedResult.exception at measurement.instant)
            }

        }
    }

    protected abstract fun convertInput(ep: ComparableQuantity<ElectricPotential>): Try<DaqcQuantity<Q>>

    override fun activate() = analogInput.activate()

    override fun deactivate() = analogInput.deactivate()
}