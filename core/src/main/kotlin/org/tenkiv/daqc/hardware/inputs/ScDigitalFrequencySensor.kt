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

    override val updateRate get() = digitalInput.updateRate

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