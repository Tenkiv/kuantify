package org.tenkiv.daqc.learning

import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.GravesLSTM
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.Input

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

internal class CorrelatedLstmNetwork(vararg inputs: Input<DaqcValue>) {

    private val correlatedInputs = inputs

    private val net: MultiLayerNetwork
    private var priorIns = Nd4j.zeros(1, correlatedInputs.size)
    private var priorOut = Nd4j.create(doubleArrayOf(100.0))

    init {
        val lstmconf = NeuralNetConfiguration.Builder().apply {
            iterations(10)
            weightInit(WeightInit.XAVIER)
            learningRate(0.5)
        }.list().backprop(true)

        lstmconf.layer(0, GravesLSTM.Builder().apply {
            nIn(correlatedInputs.size)
            nOut(correlatedInputs.size)
            activation(Activation.SIGMOID)
        }.build())

        lstmconf.layer(1, GravesLSTM.Builder().apply {
            nIn(correlatedInputs.size)
            nOut(correlatedInputs.size)
            activation(Activation.SIGMOID)
        }.build())

        lstmconf.layer(2, RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE).apply {
            nIn(correlatedInputs.size)
            nOut(1)
            activation(Activation.IDENTITY)
        }.build())

        net = MultiLayerNetwork(lstmconf.build())
        net.init()

        train()
    }

    fun run(): Float? {
        val values = getCorrelatedValues() ?: return null
        priorOut = net.output(values)
        priorIns = values
        return priorOut.getFloat(0)
    }

    fun train(desiredValue: Float, actualValue: Float) {
        when {
            desiredValue > actualValue -> train(wasHigh = false)
            desiredValue < actualValue -> train(wasHigh = true)
            else -> train()
        }
    }

    private fun train(wasHigh: Boolean) {
        var newValue = priorOut.getDouble(0)
        if (wasHigh)
            newValue--
        else
            newValue++

        if (newValue < 0)
            newValue = 0.0

        priorOut.putScalar(0, newValue)
        net.fit(priorIns, priorOut)
    }

    private fun train() {
        net.fit(priorIns, priorOut)
    }

    private fun getCorrelatedValues(): INDArray? =
            Nd4j.create(correlatedInputs.map {
                it.broadcastChannel.valueOrNull?.value?.toPidFloat() ?: return null
            }.toFloatArray())
}

