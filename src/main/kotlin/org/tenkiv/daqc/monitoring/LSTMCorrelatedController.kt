package org.tenkiv.daqc.monitoring

import kotlinx.coroutines.experimental.CommonPool
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.GravesLSTM
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions
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

class QuantityLSTMCorrelatedController<I : Quantity<I>, O : Quantity<O>>(targetInput: Input<DaqcQuantity<I>>,
                                                                         output: Output<DaqcQuantity<O>>,
                                                                         desiredValue: DaqcQuantity<I>,
                                                                         activationFun: (Input<DaqcQuantity<I>>,
                                                                                         Array<out Input<DaqcQuantity<*>>>,
                                                                                         Float) -> DaqcQuantity<O>,
                                                                         vararg correlatedInputs: Input<DaqcQuantity<*>>) :
        AbstractLSTMCorrelatedController<I, DaqcQuantity<O>>(
                targetInput = targetInput,
                output = output,
                desiredValue = desiredValue,
                activationFun = activationFun,
                correlatedInputs = *correlatedInputs)

class BinaryLSTMCorrelatedController<I : Quantity<I>, O : Output<BinaryState>>(targetInput: Input<DaqcQuantity<I>>,
                                                                               output: O,
                                                                               desiredValue: DaqcQuantity<I>,
                                                                               vararg correlatedInputs: Input<DaqcQuantity<I>>) :
        AbstractLSTMCorrelatedController<I, BinaryState>(
                targetInput = targetInput,
                output = output,
                desiredValue = desiredValue,
                activationFun = { mainInput, relatedInputs, data ->
                    if (data > 1) {
                        BinaryState.On
                    } else {
                        BinaryState.Off
                    }
                },
                correlatedInputs = *correlatedInputs)

abstract class AbstractLSTMCorrelatedController<I : Quantity<I>, out O : DaqcValue>(private val targetInput:
                                                                                    Input<DaqcQuantity<I>>,
                                                                                    private val output: Output<O>,
                                                                                    private val desiredValue: DaqcQuantity<I>,
                                                                                    private vararg val correlatedInputs:
                                                                                    Input<DaqcQuantity<*>>,
                                                                                    private val activationFun:
                                                                                    (Input<DaqcQuantity<I>>,
                                                                                     Array<out Input<DaqcQuantity<*>>>,
                                                                                     Float) -> O) {

    private val net = NeuralNetwork(3, 3, 1)

    private val correlatedNetwork = CorrelatedLSTMNetwork()

    private var error: Float = 0f
    private var previousError: Float = 0f
    private var integral: Float = 0f

    private var previousTime: Instant = Instant.now()

    private var kp = .3f
    private var ki = .4f
    private var kd = .3f

    init {
        targetInput.openNewCoroutineListener(CommonPool) {
            val recentVal = it.value.toFloatInSystemUnit()

            val time = if (previousTime.isBefore(it.instant)) {
                (Duration.between(previousTime, it.instant).seconds / 1000.0)
            } else {
                .00005
            }

            if (previousTime.isAfter(it.instant)) {
                previousTime = it.instant
            }

            error = desiredValue.toFloatInSystemUnit() - recentVal
            integral += (error * time).toFloat()
            val derivative = (error - previousError)

            println("Kp:$kp Ki:$ki Kd:$kd")
            val pid = (kp * error + ki * integral + kd * derivative)
            previousError = error
            println("CurrentTemp:$recentVal Output:$pid Error:$previousError Integral: $integral")

            val correlatedValue = if (correlatedInputs.isNotEmpty()) {
                correlatedNetwork.run()
            } else {
                0f
            }

            net.train(floatArrayOf(
                    derivative,
                    recentVal,
                    correlatedValue),
                    floatArrayOf(integral))

            kp = net.weights[1][0][0]
            ki = net.weights[1][1][0]
            kd = net.weights[1][2][0]

            if (integral > WINDUP_LIMIT) {
                integral = WINDUP_LIMIT
            } else if (integral < -WINDUP_LIMIT) {
                integral = -WINDUP_LIMIT
            }

            output.setOutput(activationFun(targetInput, correlatedInputs, pid))

            previousTime = it.instant
        }
    }

    inner class CorrelatedLSTMNetwork(vararg inputs: Input<DaqcQuantity<*>>) {
        private val net: MultiLayerNetwork
        var priorIns = Nd4j.zeros(1, 3)
        var priorOut = Nd4j.create(doubleArrayOf(100.0))

        init {
            val lstmconf = NeuralNetConfiguration.Builder()
                    .iterations(10)
                    .weightInit(WeightInit.XAVIER)
                    .learningRate(0.5)
                    .list()
                    .backprop(true)

            lstmconf.layer(0, GravesLSTM.Builder().apply {
                nIn(3)
                nOut(4)
                activation(Activation.SIGMOID)
            }.build())

            lstmconf.layer(1, GravesLSTM.Builder().apply {
                nIn(4)
                nOut(4)
                activation(Activation.SIGMOID)
            }.build())

            lstmconf.layer(2, RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE).apply {
                nIn(4)
                nOut(1)
                activation(Activation.IDENTITY)
            }.build())

            net = MultiLayerNetwork(lstmconf.build())
            net.init()

            train()
        }

        fun run(): Float {
            val values = getCorrelatedValues()
            priorOut = net.output(values)
            priorIns = values
            return priorOut.getFloat(0)
        }

        fun train(wasHigh: Boolean) {
            var newValue = priorOut.getDouble(0)
            if (wasHigh) {
                newValue--
            } else {
                newValue++
            }
            if (newValue < 0) {
                newValue = 0.0
            }
            priorOut.putScalar(0, newValue)
            net.fit(priorIns, priorOut)
        }

        fun train() {
            net.fit(priorIns, priorOut)
        }

        private fun getCorrelatedValues(): INDArray = Nd4j.create(correlatedInputs.map
        { it.broadcastChannel.value.value.toFloatInSystemUnit() }.toFloatArray())
    }

}

