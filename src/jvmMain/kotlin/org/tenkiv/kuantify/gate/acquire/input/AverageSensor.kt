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
import java.time.*
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

    @Volatile
    private var _valueOrNull: QuantityMeasurement<QT>? = run {
        val currentMeasurements = inputs.asSequence().map { it.valueOrNull }.requireNoNulls().toList()

        return@run if (currentMeasurements.isNotEmpty()) {
            // currentValues is not empty so maxBy cannot return null.
            val mostRecentMeasurement = currentMeasurements.maxBy { it.instant }!!
            val defaultUnit = mostRecentMeasurement.value.unit.default
            currentMeasurements.map { measurement ->
                measurement.value.inDefaultUnit
            }.average().toQuantity(defaultUnit).toDaqc() at mostRecentMeasurement.instant
        } else {
            null
        }
    }
    override val valueOrNull: ValueInstant<DaqcQuantity<QT>>?
        get() = _valueOrNull

    private val _isTransceiving = Updatable(false)
    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val broadcastChannel = BroadcastChannel<QuantityMeasurement<QT>>(capacity = Channel.BUFFERED)

    private val _isFinalized = Updatable(initialValue = false)
    public override val isFinalized: InitializedTrackable<Boolean>
        get() = _isFinalized

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
                    changeWatchedInput.onEachUpdate { measurement ->
                        val defaultUnit = measurement.value.unit.default

                        val currentValues = HashSet<Quantity<QT>>()
                        inputs.forEach { input ->
                            input.valueOrNull?.let { currentValues += it.value }
                        }

                        val newAverage = currentValues.map { quantity ->
                            quantity.inDefaultUnit
                        }.average().toQuantity(defaultUnit).toDaqc()

                        update(newAverage at measurement.instant)
                    }
                }

                //TODO: Consider moving this to reusable function
                launch(Dispatchers.Daqc) {
                    changeWatchedInput.isTransceiving.onEachUpdate { newStatus ->
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

    override fun openSubscription(): ReceiveChannel<ValueInstant<DaqcQuantity<QT>>> =
        broadcastChannel.openSubscription()

    public fun cancel() {
        coroutineContext.cancel()
    }

    public override fun finalize() {
        inputs.finalizeAll()
        _isFinalized.value = true
    }

    private suspend fun update(newValue: QuantityMeasurement<QT>) {
        _valueOrNull = newValue
        broadcastChannel.send(newValue)
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

    @Volatile
    private var _valueOrNull: BinaryStateMeasurement? = run {
        val currentMeasurements = inputs.asSequence().map { it.valueOrNull }.requireNoNulls().toList()
        val mostRecentInstant = currentMeasurements.maxBy { it.instant }?.instant
        val currentValues = currentMeasurements.map { it.value }

        val value = if (currentValues.filter { it == state }.size >= threshold) {
            BinaryState.High
        } else {
            BinaryState.Low
        }

        // Only way mostRecentInstant is null is if there are no measurements so we should return null instead of Low.
        if (mostRecentInstant != null) value at mostRecentInstant else null
    }
    public override val valueOrNull: BinaryStateMeasurement?
        get() = _valueOrNull

    private val broadcastChannel = BroadcastChannel<BinaryStateMeasurement>(capacity = Channel.BUFFERED)

    private val _isTransceiving = Updatable(initialValue = false)
    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _isFinalized = Updatable(initialValue = false)
    public override val isFinalized: InitializedTrackable<Boolean>
        get() = _isFinalized

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
                    changeWatchedInput.onEachUpdate { measurement ->
                        val currentValues = HashSet<BinaryState>()

                        inputs.forEach { input ->
                            input.valueOrNull?.let { currentValues += it.value }
                        }

                        update(
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
                    changeWatchedInput.isTransceiving.onEachUpdate { newStatus ->
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

    public override fun openSubscription(): ReceiveChannel<BinaryStateMeasurement> =
        broadcastChannel.openSubscription()

    public override fun startSampling() {
        inputs.forEach { it.startSampling() }
    }

    //TODO: We might not want to actually cancel the underlying inputs
    public override fun stopTransceiving() {
        inputs.forEach { it.stopTransceiving() }
    }

    public override fun finalize() {
        inputs.finalizeAll()
        _isFinalized.value = true
    }

    private suspend fun update(newValue: BinaryStateMeasurement) {
        _valueOrNull = newValue
        broadcastChannel.send(newValue)
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