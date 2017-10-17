package org.tenkiv.daqc.monitoring

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.yield
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.distribution.UniformDistribution
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.tenkiv.coral.ValueInstant
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.Input
import org.tenkiv.daqc.hardware.definitions.Output
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

private const val WINDUP_LIMIT = 20.0f


fun <I : Quantity<I>, O : Quantity<O>> createNnpidController(
        targetInput: Input<DaqcQuantity<I>>,
        output: Output<DaqcQuantity<O>>,
        postProcess: (Input<DaqcQuantity<I>>, Array<out Input<DaqcValue>>, Float) -> DaqcQuantity<O>,
        vararg correlatedInputs: Input<DaqcValue>
):
        AbstractNnpidController<DaqcQuantity<I>, DaqcQuantity<O>> =
        QuantityNnpidController(targetInput, output, postProcess, *correlatedInputs)

fun <I : Quantity<I>> createNnpidController(
        targetInput: Input<DaqcQuantity<I>>,
        output: Output<BinaryState>,
        vararg correlatedInputs: Input<DaqcValue>):
        AbstractNnpidController<DaqcQuantity<I>, BinaryState> =
        BinaryNnpidController(targetInput, output, *correlatedInputs)


private class QuantityNnpidController<I : Quantity<I>, O : Quantity<O>>(
        targetInput: Input<DaqcQuantity<I>>,
        output: Output<DaqcQuantity<O>>,
        activationFun: (Input<DaqcQuantity<I>>, Array<out Input<DaqcValue>>, Float) -> DaqcQuantity<O>,
        vararg correlatedInputs: Input<DaqcValue>
) :
        AbstractNnpidController<DaqcQuantity<I>, DaqcQuantity<O>>(
                targetInput = targetInput,
                output = output,
                activationFun = activationFun,
                correlatedInputs = *correlatedInputs
        )

private class BinaryNnpidController<I : Quantity<I>>(targetInput: Input<DaqcQuantity<I>>,
                                                     output: Output<BinaryState>,
                                                     vararg correlatedInputs: Input<DaqcValue>) :
        AbstractNnpidController<DaqcQuantity<I>, BinaryState>(
                targetInput = targetInput,
                output = output,
                activationFun = { _, _, data ->
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

    private var _isActivate = true

    private var listenJob: Job? = null

    private val correlatedNetwork = if (correlatedInputs.isNotEmpty())
        CorrelatedLstmNetwork(*correlatedInputs)
    else
        null

    private var pidEntryLayerSize = 2

    private var error = 0f
    private var previousError = 0f
    private var integral = 0f

    private var kp = .3f
    private var ki = .4f
    private var kd = .3f

    private val defaultTimeValue = .00005
    private var previousTime: Instant = Instant.now()

    private val net: MultiLayerNetwork =
            MultiLayerNetwork(NeuralNetConfiguration.Builder().apply {
                iterations(pidIterations)
                weightInit(WeightInit.XAVIER)
                learningRate(pidLearnRate)
            }.list().backprop(true).apply {
                layer(0, DenseLayer.Builder().apply {
                    nIn(if (correlatedInputs.isNotEmpty()) {
                        pidEntryLayerSize + 1
                    } else {
                        pidEntryLayerSize
                    })
                    nOut(pidHiddenSize)
                    weightInit(WeightInit.DISTRIBUTION)
                    dist(UniformDistribution(weightLowerBound, weightUpperBound))
                    activation(Activation.TANH)
                }.build())
                layer(1, OutputLayer.Builder().apply {
                    nIn(pidHiddenSize)
                    nOut(pidOutSize)
                    weightInit(WeightInit.DISTRIBUTION)
                    dist(UniformDistribution(weightLowerBound, weightUpperBound))
                    activation(Activation.TANH)
                    lossFunction(LossFunctions.LossFunction.SQUARED_LOSS)
                }.build())
            }.build())

    private fun runJob(desiredValue: I) {
        listenJob = targetInput.openNewCoroutineListener(CommonPool) {

            val pid = runPid(desiredValue, it)

            val recentVal = it.value.toPidFloat()

            correlatedNetwork?.train(desiredValue.toPidFloat(), recentVal)

            val trainArray = Nd4j.create(
                    if (correlatedNetwork != null)
                        floatArrayOf(
                                pid.third,
                                it.value.toPidFloat(),
                                correlatedNetwork.run()
                        )
                    else
                        floatArrayOf(pid.third, recentVal)
            )


            net.fit(trainArray, Nd4j.create(floatArrayOf(integral)))

            kp = net.outputLayer.params().getFloat(0, 0)
            ki = net.outputLayer.params().getFloat(0, 1)
            kd = net.outputLayer.params().getFloat(0, 2)

            if (integral > WINDUP_LIMIT)
                integral = WINDUP_LIMIT
            else if (integral < -WINDUP_LIMIT)
                integral = -WINDUP_LIMIT

            previousTime = it.instant
            yield()

            output.setOutput(
                    activationFun(
                            targetInput,
                            correlatedInputs,
                            pid.first * kp + pid.second * ki + pid.third * kd)
            )

        }
    }

    private fun getTime(instant: Instant): Double =
            if (previousTime.isBefore(instant)) {
                (Duration.between(previousTime, instant).toMillis().toDouble())
            } else {
                defaultTimeValue
            }

    private fun runPid(desiredValue: I, data: ValueInstant<DaqcValue>): Triple<Float, Float, Float> {

        val recentVal = data.value.toPidFloat()

        val time = getTime(data.instant)

        error = desiredValue.toPidFloat() - recentVal

        integral += (error * time).toFloat()
        val derivative = error - previousError

        previousError = error

        return Triple(error, integral, derivative)
    }

    override val broadcastChannel: ConflatedBroadcastChannel<out ValueInstant<I>>
        get() = ConflatedBroadcastChannel()

    override val isActive: Boolean
        get() = _isActivate

    override fun setOutput(setting: I) {
        //TODO This can occasionally consume additional resources if called during NN training.
        if (listenJob?.isActive == true) {
            listenJob?.cancel()
        }

        runJob(setting)
    }

    override fun deactivate() {
        _isActivate = false

        listenJob?.cancel()
    }

    companion object {
        private const val pidIterations = 10
        private const val pidLearnRate = .5
        private const val pidHiddenSize = 3
        private const val pidOutSize = 1
        private const val weightUpperBound = 1.0
        private const val weightLowerBound = 0.0
    }

}