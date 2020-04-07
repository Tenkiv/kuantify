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

package org.tenkiv.kuantify.hardware.inputs

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import mu.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import org.tenkiv.kuantify.trackable.*
import physikal.*

private val logger = KotlinLogging.logger {}

/**
 * Abstract class for single channel analog sensorMap.
 *
 * @param analogInput The analog input.
 * @param maximumVoltage The maximum [Voltage] for the sensor.
 * @param acceptableError The maximum acceptable error for the sensor in [Voltage].
 */
public abstract class ScAnalogSensor<QT : Quantity<QT>>(
    public val analogInput: AnalogInput<*>,
    maximumVoltage: Quantity<Voltage>,
    acceptableError: Quantity<Voltage>
) : QuantityInput<QT>, CoroutineScope by analogInput.withNewChildJob() {
    @Volatile
    private var _valueOrNull: QuantityMeasurement<QT>? = null
    override val valueOrNull: QuantityMeasurement<QT>?
        get() = _valueOrNull

    private val broadcastChannel = BroadcastChannel<QuantityMeasurement<QT>>(capacity = Channel.BUFFERED)
    private val failureBroadcastChannel = BroadcastChannel<FailedMeasurement>(capacity = 5)

    public final override val isTransceiving get() = analogInput.isTransceiving

    public override val isFinalized: InitializedTrackable<Boolean>
        get() = analogInput.isFinalized

    public final override val updateRate get() = analogInput.updateRate

    init {
        analogInput.maxElectricPotential.set(maximumVoltage)
        analogInput.maxAcceptableError.set(acceptableError)

        launch {
            analogInput.onEachUpdate { measurement ->
                when (val convertedResult = transformInput(measurement.value)) {
                    is Result.Success -> update(convertedResult.value at measurement.instant)
                    is Result.Failure -> processFailure(convertedResult.error at measurement.instant)
                }
            }
        }
    }

    private fun transformFailureMsg() = """Analog sensor based on analog input $analogInput failed to transform input.
        |The value of this input will not be updated.""".trimMargin()

    /**
     * Function to convert the [Voltage] of the analog input to a [DaqcQuantity] or return an error.
     *
     * @param voltage The [Voltage] measured by the analog input.
     * @return A [Result] of either a [DaqcQuantity] or an error.
     */
    protected abstract suspend fun transformInput(voltage: Quantity<Voltage>): Result<DaqcQuantity<QT>, ProcessFailure>

    public final override fun startSampling() {
        analogInput.startSampling()
    }

    public final override fun stopTransceiving() {
        analogInput.stopTransceiving()
    }

    public final override fun finalize() {
        analogInput.finalize()
    }

    public override fun openSubscription(): ReceiveChannel<ValueInstant<DaqcQuantity<QT>>> =
        broadcastChannel.openSubscription()

    public override fun openProcessFailureSubscription(): ReceiveChannel<FailedMeasurement>? =
        failureBroadcastChannel.openSubscription()

    public fun cancel() {
        coroutineContext.cancel()
    }

    private suspend fun update(updated: QuantityMeasurement<QT>) {
        _valueOrNull = updated
        broadcastChannel.send(updated)
    }

    private suspend fun processFailure(failure: FailedMeasurement) {
        failureBroadcastChannel.send(failure)
        logger.warn(::transformFailureMsg)
    }

}