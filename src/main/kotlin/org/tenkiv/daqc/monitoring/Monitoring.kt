package org.tenkiv.daqc.monitoring

import java.util.*

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
/**
 * A simple neural network, styled after an implementation by Cédric Beust.
 */
class NeuralNetwork(val inputSize: Int, val hiddenSize: Int, val outputSize: Int) {

    //private val actualInputSize = inputSize + 1

    // Activations for nodes
    private val activationInput = FloatArray(/*actualInputSize*/inputSize, { 1.0f })
    private val activationHidden = FloatArray(hiddenSize, { 1.0f })
    private val activationOutput = FloatArray(outputSize, { 1.0f })

    internal val weights = ArrayList<Matrix>(2)

    // Weights for momentum
    private val momentumInput = Matrix(/*actualInputSize*/inputSize, hiddenSize)
    private val momentumOutput = Matrix(hiddenSize, outputSize)

    init {
        weights.add(Matrix(/*actualInputSize*/inputSize, hiddenSize, { -> rand(-0.2f, 0.2f) }))
        weights.add(Matrix(hiddenSize, outputSize, { -> rand(-0.2f, 0.2f) }))
    }

    /**
     * Gets a random float between two values.
     */
    private fun rand(min: Float, max: Float) = random.nextFloat() * (max - min) + min

    /**
     * The activation function.
     */
    fun activate(x: Float) = (Math.tanh(x.toDouble()).toFloat())

    /**
     * The derivative of the activation function.
     */
    private fun activateDerivative(x: Float) = (1.0f - x * x)

    private fun forwardLayer(inLayer: FloatArray, outLayer: FloatArray, weightMatrix: Matrix): FloatArray {
        repeat(outLayer.size) { o ->
            var sum = 0.0f
            repeat(inLayer.size) { i ->
                sum += inLayer[i] * weightMatrix[i][o]
            }
            outLayer[o] = activate(sum)
        }
        return outLayer
    }

    private fun backwardLayer(inLayer: FloatArray,
                              outputError: FloatArray,
                              weightMatrix: Matrix,
                              momentumMatrix: Matrix,
                              learningRate: Float,
                              momentum: Float) {

        repeat(inLayer.size) { i ->
            repeat(outputError.size) { j ->
                val change = outputError[j] * inLayer[i]
                weightMatrix[i][j] = weightMatrix[i][j] + learningRate * change + momentum * momentumMatrix[i][j]
                momentumMatrix[i][j] = change
            }
        }
    }

    /**
     * Run the graph with the given inputs.
     *
     * @return the outputs as a vector.
     */
    fun runGraph(inputs: FloatArray): FloatArray {
        if (inputs.size != /*actualInputSize - 1*/inputSize) {
            throw RuntimeException("Expected ${/*actualInputSize - 1*/inputSize} inputs but got ${inputs.size}")
        }

        // Input activations (note: -1 since we don't count the bias node)
        repeat(/*actualInputSize - 1*/inputSize) {
            activationInput[it] = inputs[it]
        }

        //Hidden Activation
        forwardLayer(activationInput, activationHidden, weights[0])

        //Output Activation
        return forwardLayer(activationHidden, activationOutput, weights[1])
    }

    private fun backPropagate(targets: FloatArray, learningRate: Float, momentum: Float): Float {
        if (targets.size != outputSize) {
            throw RuntimeException("Expected $outputSize targets but got ${targets.size}")
        }

        // Calculate error terms for output
        val outputDeltas = FloatArray(outputSize, { 0f })
        repeat(outputSize) { k ->
            val error = targets[k] - activationOutput[k]
            outputDeltas[k] = activateDerivative(activationOutput[k]) * error
        }

        // Calculate error terms for hidden layers
        val hiddenDeltas = FloatArray(hiddenSize, { 0f })
        repeat(hiddenSize) { j ->
            var error = 0.0f
            repeat(outputSize) { k ->
                error += outputDeltas[k] * weights[1][j][k]
            }
            hiddenDeltas[j] = activateDerivative(activationHidden[j]) * error
        }

        // Update output weights
        backwardLayer(activationHidden, outputDeltas, weights[1], momentumOutput, learningRate, momentum)

        // Update input weights
        backwardLayer(activationInput, hiddenDeltas, weights[0], momentumInput, learningRate, momentum)

        // Calculate error
        var error = 0.0
        repeat(targets.size) { k ->
            val diff = targets[k] - activationOutput[k]
            error += 0.5 * diff * diff
        }

        return error.toFloat()
    }

    fun train(data: Array<FloatArray>, result: Array<FloatArray>, iterations: Int = 1000, learningRate: Float = .5f,
              momentum: Float = .1f) {
        repeat(iterations) {
            var error = 0.0f
            data.forEachIndexed { i, pattern ->
                runGraph(data[i])
                val bp = backPropagate(result[i], learningRate, momentum)
                error += bp
            }
        }
    }

    fun train(data: FloatArray, result: FloatArray, iterations: Int = 1000, learningRate: Float = .5f,
              momentum: Float = .1f) {
        repeat(iterations) {
            var error = 0.0f
            runGraph(data)
            val bp = backPropagate(result, learningRate, momentum)
            error += bp
        }
    }

    companion object {
        val random = Random()
    }
}

/**
 * A matrix of values which automatically initializes entries by a default function.
 */
internal class Matrix(val rows: Int, val columns: Int, defaultValue: () -> Float = { -> 0.0f }) {
    val content = ArrayList<ArrayList<Float>>(rows)

    init {
        repeat(rows) { j ->
            val nl = ArrayList<Float>()
            content.add(nl)
            repeat(columns) {
                nl.add(defaultValue())
            }
        }
    }

    operator fun get(i: Int) = content[i]
}