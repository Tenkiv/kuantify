package org.tenkiv.daqc.monitoring

import kotlinx.coroutines.experimental.CommonPool
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.hardware.definitions.channel.Input
import org.tenkiv.daqc.hardware.definitions.channel.Output
import org.tenkiv.physikal.core.toDouble
import org.tenkiv.physikal.core.tu
import shape.komputation.cpu.Network
import shape.komputation.initialization.heInitialization
import shape.komputation.layers.entry.inputLayer
import shape.komputation.layers.forward.activation.ActivationFunction
import shape.komputation.layers.forward.denseLayer
import shape.komputation.loss.squaredLoss
import shape.komputation.matrix.doubleColumnVector
import shape.komputation.optimization.stochasticGradientDescent
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

/*class QuantityNNPIDController<I : Quantity<I>, O : Quantity<O>>(private val targetInput: Input<DaqcQuantity<I>>,
                                                                private val output: Output<DaqcQuantity<O>>,
                                                                private val outputUnit: Unit<O>,
                                                                private val desiredValue: DaqcQuantity<I>,
                                                                private vararg val correlatedInputs: Input<DaqcQuantity<I>>){

    val init = heInitialization(Random())

    private val hiddenLayer = denseLayer((2+correlatedInputs.size), 3, init, init,
            ActivationFunction.Sigmoid, stochasticGradientDescent(0.1))
    private val outputLayer = denseLayer(3, 3, init, init,
            ActivationFunction.Sigmoid, stochasticGradientDescent(0.1))

    private val net = Network(inputLayer(2), hiddenLayer, outputLayer)

    private var error: Double = 0.0
    private var previousError: Double = 0.0
    private var integral: Double = 0.0

    private var previousTime: Instant = Instant.now()

    private var kp = .3
    private var ki = .4
    private var kd = .3

    init {
        targetInput.openNewCoroutineListener(CommonPool){
            val recentVal = it.value tu desiredValue.unit

            val time = if(previousTime.isBefore(it.instant)){
                (Duration.between(previousTime,it.instant).seconds/1000.0)
            }else{
                .00005
            }

            if(previousTime.isAfter(it.instant)){previousTime = it.instant}

            error = desiredValue.toDouble() - recentVal.toDouble()
            integral += error * time
            val derivative = (error - previousError)

            val pid = (kp * error + ki * integral + kd * derivative)
            previousError = error

            val correlatedValues = DoubleArray(correlatedInputs.size)

            correlatedInputs.forEachIndexed { index, input ->
                correlatedValues[index] = input.broadcastChannel.value.value.toDouble()
            }

            net.train(arrayOf(doubleColumnVector(desiredValue.toDouble(), error, *correlatedValues)),
                    arrayOf(doubleColumnVector(kp, ki, kd)),
                    squaredLoss(1), 1, 1)

            val netUpdate = net.forward(
                    doubleColumnVector(desiredValue.toDouble(), error, *correlatedValues),
                    false).entries

            kp = netUpdate[0]
            ki = netUpdate[1]
            kd = netUpdate[2]

            if(integral > WINDUP_LIMIT){
                integral = WINDUP_LIMIT
            }

            output.setOutput(DaqcQuantity.Companion.of(pid,outputUnit))

            previousTime = it.instant
        }
    }
}

class BinaryNNPIDController<I : Quantity<I>>(private val targetInput: Input<DaqcQuantity<I>>,
                                             private val output: DigitalOutput,
                                             private val desiredValue: DaqcQuantity<I>,
                                             private vararg val correlatedInputs: Input<DaqcQuantity<I>>){

    val init = heInitialization(Random())

    private val hiddenLayer = denseLayer((2+correlatedInputs.size), 3, init, init,
            ActivationFunction.Sigmoid, stochasticGradientDescent(0.1))
    private val outputLayer = denseLayer(3, 3, init, init,
            ActivationFunction.Sigmoid, stochasticGradientDescent(0.1))

    private val net = Network(inputLayer(2), hiddenLayer, outputLayer)

    private var error: Double = 0.0
    private var previousError: Double = 0.0
    private var integral: Double = 0.0

    private var previousTime: Instant = Instant.now()

    private var kp = .3
    private var ki = .4
    private var kd = .3

    init {
        targetInput.openNewCoroutineListener(CommonPool){
            val recentVal = it.value tu desiredValue.unit

            val time = if(previousTime.isBefore(it.instant)){
                (Duration.between(previousTime,it.instant).seconds/1000.0)
            }else{
                .00005
            }

            if(previousTime.isAfter(it.instant)){previousTime = it.instant}

            error = desiredValue.toDouble() - recentVal.toDouble()
            integral += error * time
            val derivative = (error - previousError)

            println("Kp:$kp Ki:$ki Kd:$kd")
            val pid = (kp * error + ki * integral + kd * derivative)
            previousError = error
            println("CurrentTemp:$recentVal Output:$pid Error:$previousError Integral: $integral")

            val correlatedValues = DoubleArray(correlatedInputs.size)

            correlatedInputs.forEachIndexed { index, input ->
                correlatedValues[index] = input.broadcastChannel.value.value.toDouble()
            }

            net.train(arrayOf(doubleColumnVector(desiredValue.toDouble(), error, *correlatedValues)),
                    arrayOf(doubleColumnVector(kp, ki, kd)),
                    squaredLoss(1), 1, 1)

            val netUpdate = net.forward(
                    doubleColumnVector(desiredValue.toDouble(), error, *correlatedValues),
                    false).entries

            kp = netUpdate[0]
            ki = netUpdate[1]
            kd = netUpdate[2]

            if(integral > WINDUP_LIMIT){
                integral = WINDUP_LIMIT
            }

            if(pid > 1){
                output.setOutput(BinaryState.On)
            }else{
                output.setOutput(BinaryState.Off)
            }

            previousTime = it.instant
        }
    }
}*/

const val WINDUP_LIMIT = 100.0

class QuantityNNPIDController<I : Quantity<I>, O : Quantity<O>>(targetInput: Input<DaqcQuantity<I>>,
                                                                output: Output<DaqcQuantity<O>>,
                                                                desiredValue: DaqcQuantity<I>,
                                                                activationFun: (Input<DaqcQuantity<I>>,
                                                                                Array<out Input<DaqcQuantity<I>>>,
                                                                                Double) -> DaqcQuantity<O>,
                                                                vararg correlatedInputs: Input<DaqcQuantity<I>>) :
        AbstractNNPIDController<I, DaqcQuantity<O>>(
                targetInput = targetInput,
                output = output,
                desiredValue = desiredValue,
                activationFun = activationFun,
                correlatedInputs = *correlatedInputs)

class BinaryNNPIDController<I : Quantity<I>>(targetInput: Input<DaqcQuantity<I>>,
                                             output: Output<DaqcValue>,
                                             desiredValue: DaqcQuantity<I>,
                                             vararg correlatedInputs: Input<DaqcQuantity<I>>) :
        AbstractNNPIDController<I, DaqcValue>(
                targetInput = targetInput,
                output = output,
                desiredValue = desiredValue,
                activationFun = { mainInput, relatedInputs, data -> BinaryState.On },
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
                                                                            Double) -> O) {

    val init = heInitialization(Random())

    private val hiddenLayer = denseLayer((2 + correlatedInputs.size), 3, init, init,
            ActivationFunction.Sigmoid, stochasticGradientDescent(0.1))
    private val outputLayer = denseLayer(3, 3, init, init,
            ActivationFunction.Sigmoid, stochasticGradientDescent(0.1))

    private val net = Network(inputLayer(2), hiddenLayer, outputLayer)

    private var error: Double = 0.0
    private var previousError: Double = 0.0
    private var integral: Double = 0.0

    private var previousTime: Instant = Instant.now()

    private var kp = .3
    private var ki = .4
    private var kd = .3

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

            error = desiredValue.toDouble() - recentVal.toDouble()
            integral += error * time
            val derivative = (error - previousError)

            println("Kp:$kp Ki:$ki Kd:$kd")
            val pid = (kp * error + ki * integral + kd * derivative)
            previousError = error
            println("CurrentTemp:$recentVal Output:$pid Error:$previousError Integral: $integral")

            val correlatedValues = DoubleArray(correlatedInputs.size)

            correlatedInputs.forEachIndexed { index, input ->
                correlatedValues[index] = input.broadcastChannel.value.value.toDouble()
            }

            net.train(arrayOf(doubleColumnVector(desiredValue.toDouble(), error, *correlatedValues)),
                    arrayOf(doubleColumnVector(kp, ki, kd)),
                    squaredLoss(1), 1, 1)

            val netUpdate = net.forward(
                    doubleColumnVector(desiredValue.toDouble(), error, *correlatedValues),
                    false).entries

            kp = netUpdate[0]
            ki = netUpdate[1]
            kd = netUpdate[2]

            if (integral > WINDUP_LIMIT) {
                integral = WINDUP_LIMIT
            }

            output.setOutput(activationFun(targetInput, correlatedInputs, pid))

            previousTime = it.instant
        }
    }
}