/*
 * Copyright 2019 Tenkiv, Inc.
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
 *
 */

package org.tenkiv.kuantify.hardware.inputs

import arrow.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.channel.*
import tec.units.indriya.*
import javax.measure.*
import javax.measure.quantity.*

/**
 * Abstract class for single channel analog sensorMap.
 *
 * @param analogInput The analog input.
 * @param maximumEp The maximum [ElectricPotential] for the sensor.
 * @param acceptableError The maximum acceptable error for the sensor in [ElectricPotential].
 */
abstract class ScAnalogSensor<Q : Quantity<Q>>(
    val analogInput: AnalogInput,
    maximumEp: ComparableQuantity<ElectricPotential>,
    acceptableError: ComparableQuantity<ElectricPotential>
) : QuantityInput<Q> {

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()
    final override val updateBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    final override val failureBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    override val isTransceiving get() = analogInput.isTransceiving

    override val updateRate get() = analogInput.updateRate

    init {
        analogInput.maxElectricPotential.set(maximumEp)
        analogInput.maxAcceptableError.set(acceptableError)

        launch {
            analogInput.updateBroadcaster.consumeEach { measurement ->
                val convertedResult = convertInput(measurement.value)

                when (convertedResult) {
                    is Success -> _broadcastChannel.send(convertedResult.value at measurement.instant)
                    is Failure -> _failureBroadcastChannel.send(convertedResult.exception at measurement.instant)
                }
            }
        }
    }

    /**
     * Function to convert the [ElectricPotential] of the analog input to a [DaqcQuantity] or return an error.
     *
     * @param ep The [ElectricPotential] measured by the analog input.
     * @return A [Try] of either a [DaqcQuantity] or an error.
     */
    protected abstract fun convertInput(ep: ComparableQuantity<ElectricPotential>): Try<DaqcQuantity<Q>>

    override fun startSampling() = analogInput.startSampling()

    override fun stopTransceiving() = analogInput.stopTransceiving()
}