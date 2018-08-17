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

package org.tenkiv.daqc.gate.receive.input

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.QuantityMeasurement
import org.tenkiv.daqc.data.toDaqc
import org.tenkiv.daqc.lib.openNewCoroutineListener
import org.tenkiv.physikal.core.averageOrNull
import tec.units.indriya.ComparableQuantity
import javax.measure.Quantity

//TODO: Make this work with BinaryState or make another version for BinaryState
class AverageQuantitySensor<Q : Quantity<Q>>(private vararg val inputs: QuantityInput<Q>) :
    QuantityInput<Q> {

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

    override val updateRate
        get() = inputs.map { it.updateRate }.max()
                ?: throw IllegalStateException("AverageQuantitySensor has no inputs, it must have at least 1 input.")

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
