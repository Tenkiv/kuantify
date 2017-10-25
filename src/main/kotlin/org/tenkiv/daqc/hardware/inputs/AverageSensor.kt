package org.tenkiv.daqc.hardware.inputs

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.QuantityInput
import org.tenkiv.daqc.QuantityMeasurement
import org.tenkiv.daqc.lib.openNewCoroutineListener
import org.tenkiv.daqc.toDaqc
import org.tenkiv.physikal.core.averageOrNull
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity

//TODO: Make this work with BinaryState or make another version for BinaryState
class AverageQuantitySensor<Q : Quantity<Q>>(vararg private val inputs: QuantityInput<Q>) : QuantityInput<Q> {

    override val isActive: Boolean
        get() = run {
            inputs.forEach {
                if (!it.isActive)
                    return@run false
            }
            return true
        }

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()
    override val broadcastChannel: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    init {
        inputs.forEach { _ ->
            openNewCoroutineListener(CommonPool) { measurement ->
                val currentValues = HashSet<ComparableQuantity<Q>>()

                inputs.forEach { input ->
                    input.broadcastChannel.valueOrNull?.let { currentValues += it.value }
                }

                currentValues.averageOrNull { it }?.let {
                    _broadcastChannel.send(it.toDaqc() at measurement.instant)
                }
            }

            failureBroadcastChannel.openNewCoroutineListener(CommonPool) {
                _failureBroadcastChannel.send(it)
            }
        }
    }

    override fun activate() = inputs.forEach { it.activate() }

    override fun deactivate() = inputs.forEach { it.deactivate() }
}
