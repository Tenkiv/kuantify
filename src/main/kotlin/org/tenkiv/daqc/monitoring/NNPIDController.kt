package org.tenkiv.daqc.monitoring

import kotlinx.coroutines.experimental.CommonPool
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.channel.Input
import org.tenkiv.daqc.hardware.definitions.channel.Output
import org.tenkiv.physikal.core.toFloat
import org.tenkiv.physikal.core.tu
import java.time.Duration
import java.time.Instant
import java.util.*
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

class QuantityNNPIDController<I : Quantity<I>, O : Quantity<O>>(targetInput: Input<DaqcQuantity<I>>,
                                                                output: Output<DaqcQuantity<O>>,
                                                                desiredValue: DaqcQuantity<I>,
                                                                activationFun: (Input<DaqcQuantity<I>>,
                                                                                Array<out Input<DaqcQuantity<I>>>,
                                                                                Float) -> DaqcQuantity<O>,
                                                                vararg correlatedInputs: Input<DaqcQuantity<I>>) :
        AbstractNNPIDController<I, DaqcQuantity<O>>(
                targetInput = targetInput,
                output = output,
                desiredValue = desiredValue,
                activationFun = activationFun,
                correlatedInputs = *correlatedInputs)

class BinaryNNPIDController<I : Quantity<I>, O : Output<BinaryState>>(targetInput: Input<DaqcQuantity<I>>,
                                                                      output: O,
                                                                      desiredValue: DaqcQuantity<I>,
                                                                      vararg correlatedInputs: Input<DaqcQuantity<I>>) :
        AbstractNNPIDController<I, BinaryState>(
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

abstract class AbstractNNPIDController<I : Quantity<I>, out O : DaqcValue>(private val targetInput:
                                                                           Input<DaqcQuantity<I>>,
                                                                           private val output: Output<O>,
                                                                           private val desiredValue: DaqcQuantity<I>,
                                                                           private vararg val correlatedInputs:
                                                                           Input<DaqcQuantity<I>>,
                                                                           private val activationFun:
                                                                           (Input<DaqcQuantity<I>>,
                                                                            Array<out Input<DaqcQuantity<I>>>,
                                                                            Float) -> O) {

    private val inputSize = 2 + correlatedInputs.size

    private val net = NeuralNetwork(2 + correlatedInputs.size, 3, 1)

    private var error: Float = 0f
    private var previousError: Float = 0f
    private var integral: Float = 0f

    private var previousTime: Instant = Instant.now()

    private var kp = .3f
    private var ki = .4f
    private var kd = .3f

    init {
        targetInput.openNewCoroutineListener(CommonPool) {
            val recentVal = it.value tu desiredValue.unit

            val time = if (previousTime.isBefore(it.instant)) {
                (Duration.between(previousTime, it.instant).seconds / 1000.0)
            } else {
                .00005
            }

            if (previousTime.isAfter(it.instant)) {
                previousTime = it.instant
            }

            error = desiredValue.toFloat() - recentVal.toFloat()
            integral += (error * time).toFloat()
            val derivative = (error - previousError)

            //println("Kp:$kp Ki:$ki Kd:$kd")
            val pid = (kp * error + ki * integral + kd * derivative).toFloat()
            previousError = error
            //println("CurrentTemp:$recentVal Output:$pid Error:$previousError Integral: $integral")

            val correlatedValues = FloatArray(correlatedInputs.size)

            correlatedInputs.forEachIndexed { index, input ->
                correlatedValues[index] = input.broadcastChannel.value.value.toFloat()
            }

            net.train(floatArrayOf(
                    desiredValue.toFloat(),
                    recentVal.toFloat(),
                    *correlatedValues),
                    floatArrayOf(error))

            net.runGraph(floatArrayOf(desiredValue.toFloat(), error, *correlatedValues))

            kp = net.weightOutput[0][0]
            ki = net.weightOutput[1][0]
            kd = net.weightOutput[2][0]

            if (integral > WINDUP_LIMIT) {
                integral = WINDUP_LIMIT
            } else if (integral < -WINDUP_LIMIT) {
                integral = -WINDUP_LIMIT
            }

            output.setOutput(activationFun(targetInput, correlatedInputs, pid))

            previousTime = it.instant
        }
    }
}

/**
 * A simple neural network, styled after an implementation by Cédric Beust <cedric@beust.com>.
 */
class NeuralNetwork(val inputSize: Int, val hiddenSize: Int, val outputSize: Int) {

    private val actualInputSize = inputSize + 1

    // Activations for nodes
    private val activationInput = FloatArray(actualInputSize, { 1.0f })
    private val activationHidden = FloatArray(hiddenSize, { 1.0f })
    private val activationOutput = FloatArray(outputSize, { 1.0f })

    internal val weightInput = Matrix(actualInputSize, hiddenSize, { -> rand(-0.2f, 0.2f) })
    internal val weightOutput = Matrix(hiddenSize, outputSize, { -> rand(-0.2f, 0.2f) })

    // Weights for momentum
    private val momentumInput = Matrix(actualInputSize, hiddenSize)
    private val momentumOutput = Matrix(hiddenSize, outputSize)

    /**
     * Gets a random float between two values.
     */
    private fun rand(min: Float, max: Float) = random.nextFloat() * (max - min) + min

    /**
     * The activation function.
     */
    private fun activate(x: Float) = (Math.tanh(x.toDouble()).toFloat())

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
        if (inputs.size != actualInputSize - 1) {
            throw RuntimeException("Expected ${actualInputSize - 1} inputs but got ${inputs.size}")
        }

        // Input activations (note: -1 since we don't count the bias node)
        repeat(actualInputSize - 1) {
            activationInput[it] = inputs[it]
        }

        //Hidden Activation
        forwardLayer(activationInput, activationHidden, weightInput)

        //Output Activation
        return forwardLayer(activationHidden, activationOutput, weightOutput)
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
                error += outputDeltas[k] * weightOutput[j][k]
            }
            hiddenDeltas[j] = activateDerivative(activationHidden[j]) * error
        }

        // Update output weights
        backwardLayer(activationHidden, outputDeltas, weightOutput, momentumOutput, learningRate, momentum)

        // Update input weights
        backwardLayer(activationInput, hiddenDeltas, weightInput, momentumInput, learningRate, momentum)

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