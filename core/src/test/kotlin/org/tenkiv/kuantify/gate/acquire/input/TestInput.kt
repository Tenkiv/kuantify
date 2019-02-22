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

package org.tenkiv.kuantify.gate.acquire.input

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.time.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.device.*
import org.tenkiv.physikal.core.*
import javax.measure.quantity.*
import kotlin.coroutines.*
import kotlin.random.*
import kotlin.reflect.*

class TestLocalTemperatureInput(private val scope: CoroutineScope, override val uid: String) :
    LocalQuantityInput<Temperature> {

    @Volatile
    private var produceJob: Job? = null

    override val failureBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>> =
        ConflatedBroadcastChannel()

    private val _updateBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Temperature>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Temperature>>
        get() = _updateBroadcaster

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    override val updateRate: UpdateRate by runningAverage()

    override val basePath: Path = listOf(RC.DAQC_GATE, uid)

    override val isTransceiving: InitializedTrackable<Boolean> = Updatable(false)

    override fun startSampling() {
        if (produceJob?.isActive != true) produceJob = startProducing()
    }

    override fun stopTransceiving() {
        produceJob?.cancel()
    }

    private fun startProducing() = launch {
        loop {
            delay(50.millisSpan)
            _updateBroadcaster.send(getRandomTemperature().now())
        }
    }

    private fun getRandomTemperature(): DaqcQuantity<Temperature> {
        return Random.nextDouble(0.0, 100.0).celsius.toDaqc()
    }
}

class TestRemoteTemperatureInput(scope: CoroutineScope, uid: String) : FSRemoteQuantityInput<Temperature>(scope, uid) {

    override val quantityType: KClass<Temperature> = Temperature::class

    override val updateRate: UpdateRate by runningAverage()

}