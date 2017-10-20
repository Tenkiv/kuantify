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
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.Input
import org.tenkiv.daqc.hardware.definitions.Output
import org.tenkiv.daqc.hardware.definitions.QuantityOutput
import org.tenkiv.physikal.core.invoke
import tec.uom.se.unit.Units
import java.time.Duration
import java.time.Instant
import javax.measure.Quantity
import javax.measure.Unit

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

class NnpidController<I : DaqcValue, O : Quantity<O>> @PublishedApi internal constructor(
        private val targetInput: Input<I>,
        private val outputUnit: Unit<O>,
        private val output: Output<DaqcQuantity<O>>,
        private val processor:
        (Input<I>, Array<out Input<*>>, DaqcQuantity<O>) -> DaqcQuantity<O>,
        private vararg val correlatedInputs:
        Input<DaqcValue>
) : Output<I> {
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

    private var previousTime: Instant = Instant.now()

    private val net: MultiLayerNetwork =
            MultiLayerNetwork(NeuralNetConfiguration.Builder().apply {
                iterations(PID_ITERATIONS)
                weightInit(WeightInit.XAVIER)
                learningRate(PID_LEARNING_RATE)
            }.list().backprop(true).apply {
                layer(0, DenseLayer.Builder().apply {
                    nIn(if (correlatedInputs.isNotEmpty()) {
                        pidEntryLayerSize + 1
                    } else {
                        pidEntryLayerSize
                    })
                    nOut(PID_HIDDEN_SIZE)
                    weightInit(WeightInit.DISTRIBUTION)
                    dist(UniformDistribution(WEIGHT_LOWER_BOUND, WEIGHT_UPPER_BOUND))
                    activation(Activation.TANH)
                }.build())
                layer(1, OutputLayer.Builder().apply {
                    nIn(PID_HIDDEN_SIZE)
                    nOut(PID_OUT_SIZE)
                    weightInit(WeightInit.DISTRIBUTION)
                    dist(UniformDistribution(WEIGHT_LOWER_BOUND, WEIGHT_UPPER_BOUND))
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
                    processor(
                            targetInput,
                            correlatedInputs,
                            DaqcQuantity.of((pid.first * kp + pid.second * ki + pid.third * kd)(outputUnit))
                    )
            )
        }
    }

    private fun getTime(instant: Instant): Double =
            if (previousTime.isBefore(instant)) {
                (Duration.between(previousTime, instant).toMillis().toDouble())
            } else {
                DEFAULT_TIME_VALUE
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
        private const val PID_ITERATIONS = 10

        private const val PID_LEARNING_RATE = .5

        private const val PID_HIDDEN_SIZE = 3

        private const val PID_OUT_SIZE = 1

        private const val WEIGHT_UPPER_BOUND = 1.0

        private const val WEIGHT_LOWER_BOUND = 0.0

        private const val DEFAULT_TIME_VALUE = .00005

        private const val WINDUP_LIMIT = 20.0f

        inline operator fun <I : DaqcValue, reified O : Quantity<O>> invoke(
                targetInput: Input<I>,
                output: QuantityOutput<O>,
                noinline postProcessor: (Input<I>, Array<out Input<*>>, DaqcQuantity<O>) -> DaqcQuantity<O>,
                vararg correlatedInputs: Input<DaqcValue>): NnpidController<I, O> =
                NnpidController<I, O>(
                        targetInput = targetInput,
                        outputUnit = Units.getInstance().getUnit(O::class.java),
                        output = output,
                        processor = postProcessor,
                        correlatedInputs = *correlatedInputs
                )
    }
}