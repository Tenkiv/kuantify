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

package org.tenkiv.kuantify.gate.acquire.input

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import javax.measure.*
import kotlin.coroutines.*

/**
 * Sensor which provides an average of [Quantity] values from a group inputs.
 * All inputs must be of the same [Quantity] type.
 *
 * @param inputs The inputs to be averaged together.
 *
 */
class AverageQuantitySensor<Q : Quantity<Q>> internal constructor(
    scope: CoroutineScope,
    private vararg val inputs: QuantityInput<Q>
) : QuantityInput<Q> {

    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    override val isTransceiving: Boolean
        get() {
            inputs.forEach {
                if (it.isTransceiving && this.isActive) return true
            }
            return false
        }

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<Q>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Q>>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    override val updateRate by runningAverage()

    init {
        inputs.forEach { changeWatchedInput ->
            launch {
                changeWatchedInput.updateBroadcaster.consumeEach { measurement ->
                    val currentValues = HashSet<ComparableQuantity<Q>>()

                    inputs.forEach { input ->
                        input.updateBroadcaster.valueOrNull?.let { currentValues += it.value }
                    }

                    currentValues.averageOrNull { it }?.let {
                        _broadcastChannel.send(it.toDaqc() at measurement.instant)
                    }
                }
            }

            launch {
                failureBroadcastChannel.consumeEach {
                    _failureBroadcastChannel.send(it)
                }
            }
        }
    }

    override fun startSampling() = inputs.forEach { it.startSampling() }

    //TODO: We might not want to actually cancel the underlying inputs
    override fun stopTransceiving() = inputs.forEach { it.stopTransceiving() }

    fun cancel() = job.cancel()
}

fun <Q : Quantity<Q>> CoroutineScope.AverageQuantitySensor(vararg inputs: QuantityInput<Q>): AverageQuantitySensor<Q> =
    AverageQuantitySensor(this, *inputs)

/**
 * Sensor which notifies if the number of inputs in the group of [BinaryInput]s are toggled to the designated state.
 * The [BinaryState] equivalent of [AverageQuantitySensor].
 *
 * @param inputs The [BinaryInput]s for which samples are to be drawn.
 * @param threshold The minimum number of [BinaryInput]s which need to be in the desired state.
 * @param state The state for which the [BinaryInput]s should be checked. Default is [BinaryState.On].
 */
class BinaryThresholdSensor internal constructor(
    scope: CoroutineScope,
    threshold: Int,
    state: BinaryState = BinaryState.On,
    private vararg val inputs: BinaryStateInput
) : BinaryStateInput {

    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    override val isTransceiving: Boolean
        get() {
            inputs.forEach {
                if (it.isTransceiving && this.isActive) return true
            }
            return false
        }

    private val _broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _broadcastChannel

    private val _failureBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcastChannel

    override val updateRate by runningAverage()

    init {
        inputs.forEach { changeWatchedInput ->
            launch {
                changeWatchedInput.updateBroadcaster.consumeEach { measurement ->
                    val currentValues = HashSet<BinaryState>()

                    inputs.forEach { input ->
                        input.updateBroadcaster.valueOrNull?.let { currentValues += it.value }
                    }

                    _broadcastChannel.send(
                        if (currentValues.filter { it == state }.size >= threshold) {
                            BinaryState.On
                        } else {
                            BinaryState.Off
                        } at measurement.instant
                    )
                }
            }

            launch {
                failureBroadcastChannel.consumeEach {
                    _failureBroadcastChannel.send(it)
                }
            }
        }
    }

    override fun startSampling() = inputs.forEach { it.startSampling() }

    override fun stopTransceiving() = inputs.forEach { it.stopTransceiving() }
}

fun CoroutineScope.BinaryThresholdSensor(
    threshold: Int,
    state: BinaryState = BinaryState.On,
    vararg inputs: BinaryStateInput
): BinaryThresholdSensor = BinaryThresholdSensor(this, threshold, state, *inputs)