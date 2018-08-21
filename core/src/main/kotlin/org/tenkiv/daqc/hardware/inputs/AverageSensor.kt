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

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.*
import org.tenkiv.daqc.lib.openNewCoroutineListener
import org.tenkiv.physikal.core.averageOrNull
import tec.units.indriya.ComparableQuantity
import javax.measure.Quantity


/**
 * Sensor which provides an average of [Quantity] values from a group inputs.
 * All inputs must be of the same [Quantity] type.
 *
 * @param inputs The inputs to be averaged together.
 * TODO: Make this work with BinaryState or make another version for BinaryState
 */
class AverageQuantitySensor<Q : Quantity<Q>>(private vararg val inputs: QuantityInput<Q>) : QuantityInput<Q> {

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

    override val sampleRate
        get() = inputs.map { it.sampleRate }.max()
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

/**
 * Sensor which notifies if the number of inputs in the group of [BinaryInput]s are toggled to the designated state.
 * The [BinaryState] equivalent of [AverageQuantitySensor].
 *
 * @param inputs The [BinaryInput]s for which samples are to be drawn.
 * @param threshold The minimum number of [BinaryInput]s which need to be in the desired state.
 * @param state The state for which the [BinaryInput]s should be checked. Default is [BinaryState.On].
 */
class DigitalThresholdSensor(
    private vararg val inputs: BinaryInput,
    threshold: Int,
    state: BinaryState = BinaryState.On
) : BinaryInput {

    override val isActive: Boolean
        get() = run {
            inputs.forEach {
                if (!it.isActive)
                    return@run false
            }
            return true
        }

    private val _broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    override val broadcastChannel: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    override val sampleRate
        get() = inputs.map { it.sampleRate }.max()
            ?: throw IllegalStateException("AverageQuantitySensor has no inputs, it must have at least 1 input.")

    init {
        inputs.forEach { _ ->
            openNewCoroutineListener(CommonPool) { measurement ->
                val currentValues = HashSet<BinaryState>()

                inputs.forEach { input ->
                    input.broadcastChannel.valueOrNull?.let { currentValues += it.value }
                }

                _broadcastChannel.send(
                    if (currentValues.filter { it == state }.size >= threshold) {
                        BinaryState.On
                    } else {
                        BinaryState.Off
                    } at measurement.instant
                )
            }

            failureBroadcastChannel.openNewCoroutineListener(CommonPool) {
                _failureBroadcastChannel.send(it)
            }
        }
    }

    override fun activate() = inputs.forEach { it.activate() }

    override fun deactivate() = inputs.forEach { it.deactivate() }
}