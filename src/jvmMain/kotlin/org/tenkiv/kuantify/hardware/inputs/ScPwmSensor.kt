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
import physikal.types.*

private val logger = KotlinLogging.logger {}

/**
 * Abstract class for an input which takes percentage PWM data from a single digital input.
 *
 * @param digitalInput The digital input
 */
public abstract class ScPwmSensor<QT : Quantity<QT>>(val digitalInput: DigitalInput<*>) : QuantityInput<QT> {
    @Volatile
    private var _valueOrNull: ValueInstant<DaqcQuantity<QT>>? = null
    override val valueOrNull: ValueInstant<DaqcQuantity<QT>>?
        get() = _valueOrNull

    private val broadcastChannel = BroadcastChannel<QuantityMeasurement<QT>>(capacity = Channel.BUFFERED)

    private val processFailureBroadcaster = BroadcastChannel<FailedMeasurement>(capacity = 5)

    public final override val isTransceiving get() = digitalInput.isTransceivingFrequency

    public final override val updateRate get() = digitalInput.updateRate

    public val avgFrequency: UpdatableQuantity<Frequency> get() = digitalInput.avgFrequency

    public override val isFinalized: InitializedTrackable<Boolean>
        get() = digitalInput.isFinalized

    init {
        launch {
            digitalInput.openPwmSubscription().consumingOnEach { measurement ->

                when (val convertedInput = transformInput(measurement.value)) {
                    is Result.Success -> update(convertedInput.value at measurement.instant)
                    is Result.Failure -> processFailure(convertedInput.error at measurement.instant)
                }
            }
        }
    }

    /**
     * Function to convert the [Frequency] of the digital input to a [DaqcQuantity] or return an error.
     *
     * @param frequency The [Frequency] measured by the digital input.
     * @return A [Result] of either a [DaqcQuantity] or an error.
     */
    protected abstract fun transformInput(frequency: Quantity<Dimensionless>): Result<DaqcQuantity<QT>, ProcessFailure>

    public final override fun startSampling() {
        digitalInput.startSamplingPwm()
    }

    public final override fun stopTransceiving() {
        digitalInput.stopTransceiving()
    }

    public override fun openSubscription(): ReceiveChannel<ValueInstant<DaqcQuantity<QT>>> =
        broadcastChannel.openSubscription()

    public override fun openProcessFailureSubscription(): ReceiveChannel<FailedMeasurement>? =
        processFailureBroadcaster.openSubscription()

    public override fun finalize() {
        digitalInput.finalize()
    }

    private suspend fun update(updated: QuantityMeasurement<QT>) {
        _valueOrNull = updated
        broadcastChannel.send(updated)
    }

    private suspend fun processFailure(failure: FailedMeasurement) {
        logger.warn { transformFailureMsg() }
        processFailureBroadcaster.send(failure)
    }

    private fun transformFailureMsg() = """Frequency sensor based on analog input $digitalInput failed to transform 
        |input.The value of this input will not be updated.""".trimToSingleLine()
}