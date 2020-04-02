/*
 * Copyright 2020 Tenkiv, Inc.
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
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.trackable.*
import physikal.*
import kotlin.coroutines.*

/**
 * Sensor which provides an average of [Quantity] values from a group inputs.
 * All inputs must be of the same [Quantity] type.
 *
 * @param inputs The inputs to be averaged together.
 *
 */
public class AverageQuantitySensor<QT : Quantity<QT>> internal constructor(
    scope: CoroutineScope,
    private vararg val inputs: QuantityInput<QT>
) : QuantityInput<QT> {

    private val job = Job(scope.coroutineContext[Job])

    public override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    private val _isTransceiving = Updatable(false)
    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _broadcastChannel = ConflatedBroadcastChannel<QuantityMeasurement<QT>>()
    public override val updateBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<QT>>
        get() = _broadcastChannel

    public override val updateRate by runningAverage()

    init {
        launch(Dispatchers.Daqc) {
            val transceivingStatuses = HashMap<QuantityInput<QT>, Boolean>().apply {
                inputs.forEach { input ->
                    put(input, input.isTransceiving.value)
                }
            }

            inputs.forEach { changeWatchedInput ->
                launch(Dispatchers.Default) {
                    changeWatchedInput.updateBroadcaster.consumeEach { measurement ->
                        val currentValues = HashSet<Quantity<QT>>()
                        val defaultUnit = measurement.value.unit.default

                        inputs.forEach { input ->
                            input.updateBroadcaster.valueOrNull?.let { currentValues += it.value }
                        }

                        val newAverage = currentValues.map { quantity ->
                            quantity.inDefaultUnit
                        }.average().toQuantity(defaultUnit).toDaqc()

                        _broadcastChannel.send(newAverage at measurement.instant)
                    }
                }

                //TODO: Consider moving this to reusable function
                launch(Dispatchers.Daqc) {
                    changeWatchedInput.isTransceiving.updateBroadcaster.consumeEach { newStatus ->
                        transceivingStatuses += changeWatchedInput to newStatus
                        if (_isTransceiving.value && !transceivingStatuses.values.contains(true)) {
                            _isTransceiving.value = false
                        } else if (!_isTransceiving.value && transceivingStatuses.values.contains(true)) {
                            _isTransceiving.value = true
                        }
                    }
                }
            }
        }
        job.invokeOnCompletion {
            _isTransceiving.value = false
        }
    }

    public override fun startSampling() {
        inputs.forEach { it.startSampling() }
    }

    //TODO: We might not want to actually cancel the underlying inputs
    public override fun stopTransceiving() {
        inputs.forEach { it.stopTransceiving() }
    }

    fun cancel() {
        job.cancel()
    }
}

public fun <Q : Quantity<Q>> CoroutineScope.AverageQuantitySensor(
    vararg inputs: QuantityInput<Q>
): AverageQuantitySensor<Q> = AverageQuantitySensor(this, *inputs)

/**
 * Sensor which notifies if the number of inputs in the group of [BinaryInput]s are toggled to the designated state.
 * The [BinaryState] equivalent of [AverageQuantitySensor].
 *
 * @param inputs The [BinaryInput]s for which samples are to be drawn.
 * @param threshold The minimum number of [BinaryInput]s which need to be in the desired state.
 * @param state The state for which the [BinaryInput]s should be checked. Default is [BinaryState.High].
 */
public class BinaryThresholdSensor internal constructor(
    scope: CoroutineScope,
    threshold: Int,
    state: BinaryState = BinaryState.High,
    private vararg val inputs: BinaryStateInput
) : BinaryStateInput {

    private val job = Job(scope.coroutineContext[Job])

    public override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    private val _isTransceiving = Updatable(false)
    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    public override val updateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _broadcastChannel

    public override val updateRate by runningAverage()

    init {
        launch(Dispatchers.Daqc) {
            val transceivingStatuses = HashMap<BinaryStateInput, Boolean>().apply {
                inputs.forEach { input ->
                    put(input, input.isTransceiving.value)
                }
            }

            inputs.forEach { changeWatchedInput ->
                launch(Dispatchers.Default) {
                    changeWatchedInput.updateBroadcaster.consumeEach { measurement ->
                        val currentValues = HashSet<BinaryState>()

                        inputs.forEach { input ->
                            input.updateBroadcaster.valueOrNull?.let { currentValues += it.value }
                        }

                        _broadcastChannel.send(
                            if (currentValues.filter { it == state }.size >= threshold) {
                                BinaryState.High
                            } else {
                                BinaryState.Low
                            } at measurement.instant
                        )
                    }
                }

                //TODO: Consider moving this to reusable function
                launch(Dispatchers.Daqc) {
                    changeWatchedInput.isTransceiving.updateBroadcaster.consumeEach { newStatus ->
                        transceivingStatuses += changeWatchedInput to newStatus
                        if (_isTransceiving.value && !transceivingStatuses.values.contains(true)) {
                            _isTransceiving.value = false
                        } else if (!_isTransceiving.value && transceivingStatuses.values.contains(true)) {
                            _isTransceiving.value = true
                        }
                    }
                }
            }
        }

        job.invokeOnCompletion {
            _isTransceiving.value = false
        }
    }

    public override fun startSampling() {
        inputs.forEach { it.startSampling() }
    }

    //TODO: We might not want to actually cancel the underlying inputs
    public override fun stopTransceiving() {
        inputs.forEach { it.stopTransceiving() }
    }


    fun cancel() {
        job.cancel()
    }
}

public fun CoroutineScope.BinaryThresholdSensor(
    threshold: Int,
    state: BinaryState = BinaryState.High,
    vararg inputs: BinaryStateInput
): BinaryThresholdSensor = BinaryThresholdSensor(this, threshold, state, *inputs)