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
import tec.units.indriya.ComparableQuantity
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

    override val updateRate get() = analogInput.updateRate

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