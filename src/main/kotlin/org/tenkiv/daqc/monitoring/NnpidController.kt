package org.tenkiv.daqc.monitoring

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.Input
import org.tenkiv.daqc.hardware.definitions.Output
import org.tenkiv.physikal.core.toFloatInSystemUnit
import java.time.Duration
import java.time.Instant
import javax.measure.Quantity

/**
 * Copyright 2017 TENKIV, INC.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

const val WINDUP_LIMIT = 20.0f


fun <I : Quantity<I>, O : Quantity<O>> createNnpidController(
        targetInput: Input<DaqcQuantity<I>>,
        output: Output<DaqcQuantity<O>>,
        activationFun: (Input<DaqcQuantity<I>>,
                        Array<out Input<DaqcValue>>,
                        Float) -> DaqcQuantity<O>,
        vararg correlatedInputs: Input<DaqcQuantity<*>>):
        AbstractNnpidController<DaqcQuantity<I>, DaqcQuantity<O>> =
        QuantityNnpidController(targetInput, output, activationFun, *correlatedInputs)

fun <I : Quantity<I>> createNnpidController(
        targetInput: Input<DaqcQuantity<I>>,
        output: Output<BinaryState>,
        vararg correlatedInputs: Input<DaqcValue>):
        AbstractNnpidController<DaqcQuantity<I>, BinaryState>
        = BinaryNnpidController(targetInput, output, *correlatedInputs)


private class QuantityNnpidController<I : Quantity<I>, O : Quantity<O>>(targetInput: Input<DaqcQuantity<I>>,
                                                                        output: Output<DaqcQuantity<O>>,
                                                                        activationFun: (Input<DaqcQuantity<I>>,
                                                                                        Array<out Input<DaqcValue>>,
                                                                                        Float) -> DaqcQuantity<O>,
                                                                        vararg correlatedInputs: Input<DaqcValue>) :
        AbstractNnpidController<DaqcQuantity<I>, DaqcQuantity<O>>(
                targetInput = targetInput,
                output = output,
                activationFun = activationFun,
                correlatedInputs = *correlatedInputs)

private class BinaryNnpidController<I : Quantity<I>>(targetInput: Input<DaqcQuantity<I>>,
                                                     output: Output<BinaryState>,
                                                     vararg correlatedInputs: Input<DaqcValue>) :
        AbstractNnpidController<DaqcQuantity<I>, BinaryState>(
                targetInput = targetInput,
                output = output,
                activationFun = { mainInput, relatedInputs, data ->
                    if (data > 1) {
                        BinaryState.On
                    } else {
                        BinaryState.Off
                    }
                },
                correlatedInputs = *correlatedInputs)

abstract class AbstractNnpidController<I : DaqcValue, out O : DaqcValue>(private val targetInput: Input<I>,
                                                                         private val output: Output<O>,
                                                                         private val activationFun:
                                                                         (Input<I>,
                                                                          Array<out Input<DaqcValue>>,
                                                                          Float) -> O,
                                                                         private vararg val correlatedInputs:
                                                                         Input<DaqcValue>) : Output<I> {

    private var isActivated = true

    private var listenJob: Job? = null

    private val correlatedNetwork = if (correlatedInputs.isNotEmpty()) {
        CorrelatedLstmNetwork(*correlatedInputs)
    } else {
        null
    }

    private val net = NeuralNetwork(2 + if (correlatedNetwork != null) {
        1
    } else {
        0
    }, 3, 1)

    private var previousTime: Instant = Instant.now()

    private var error = 0f
    private var previousError = 0f
    private var integral = 0f

    private var kp = .3f
    private var ki = .4f
    private var kd = .3f

    var desiredValue: I? = null

    private fun start() {

        if (desiredValue == null) {
            throw UninitializedPropertyAccessException()
        }

        listenJob = targetInput.openNewCoroutineListener(CommonPool) {

            val pid = runPid(it)

            val recentVal = it.value.toPidFloat()

            val trainArray = if (correlatedNetwork != null) {
                floatArrayOf(
                        pid.third,
                        it.value.toPidFloat(),
                        correlatedNetwork.run())
            } else {
                floatArrayOf(
                        pid.third,
                        recentVal)
            }

            net.train(trainArray, floatArrayOf(integral))

            net.train(trainArray,
                    floatArrayOf(integral))

            kp = net.weights[1][0][0]
            ki = net.weights[1][1][0]
            kd = net.weights[1][2][0]

            if (integral > WINDUP_LIMIT) {
                integral = WINDUP_LIMIT
            } else if (integral < -WINDUP_LIMIT) {
                integral = -WINDUP_LIMIT
            }

            output.setOutput(
                    activationFun(
                            targetInput,
                            correlatedInputs,
                            pid.first * kp + pid.second * ki + pid.third * kd))

            previousTime = it.instant
        }
    }

    private fun getTime(instant: Instant): Double =
            if (previousTime.isBefore(instant)) {
                (Duration.between(previousTime, instant).seconds / 1000.0)
            } else {
                .00005
            }

    private fun runPid(data: ValueInstant<DaqcValue>): Triple<Float, Float, Float> {

        val recentVal = data.value.toPidFloat()

        val time = getTime(data.instant)

        error = when (desiredValue) {
            is DaqcQuantity<*> -> ((data.value as? DaqcQuantity<*>)
                    ?: throw Exception()).toFloatInSystemUnit()
            BinaryState.On -> 1f
            BinaryState.Off -> 0f
            else -> throw UninitializedPropertyAccessException()
        } - recentVal

        integral += (error * time).toFloat()
        val derivative = (error - previousError)

        previousError = error

        return Triple(error, integral, derivative)
    }

    override val broadcastChannel: ConflatedBroadcastChannel<out ValueInstant<I>>
        get() = ConflatedBroadcastChannel()

    override val isActive: Boolean
        get() = isActivated

    override fun setOutput(setting: I) {
        desiredValue = setting
    }

    override fun deactivate() {
        isActivated = false

        listenJob?.cancel()
    }
}