package org.tenkiv.daqc.hardware.inputs

import com.github.kittinunf.result.Result
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

    init {
        analogInput.maxElectricPotential = maximumEp
        analogInput.maxAcceptableError = acceptableError
        analogInput.openNewCoroutineListener(CommonPool) { measurement ->

            val convertedResult = convertInput(measurement.value)

            when (convertedResult) {
                is Result.Success -> _broadcastChannel.send(convertedResult.value at measurement.instant)
                is Result.Failure -> _failureBroadcastChannel.send(convertedResult.error at measurement.instant)
            }

        }
    }

    abstract protected fun convertInput(ep: ComparableQuantity<ElectricPotential>): Result<DaqcQuantity<Q>, Exception>

    override fun activate() = analogInput.activate()

    override fun deactivate() = analogInput.deactivate()
}