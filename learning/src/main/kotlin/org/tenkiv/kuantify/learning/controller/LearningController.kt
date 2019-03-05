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

package org.tenkiv.kuantify.learning.controller

import com.google.common.collect.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.time.*
import org.deeplearning4j.rl4j.learning.sync.qlearning.*
import org.deeplearning4j.rl4j.network.dqn.*
import org.deeplearning4j.rl4j.space.*
import org.deeplearning4j.rl4j.util.*
import org.nd4j.linalg.learning.config.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.learning.lib.*
import org.tenkiv.kuantify.recording.*
import org.tenkiv.physikal.core.*
import java.time.*
import kotlin.coroutines.*

//TODO: Make correlatedInputs optional.
class LearningController<T> internal constructor(
    scope: CoroutineScope,
    targetInput: RangedInput<T>,
    correlatedInputs: Collection<RangedInput<*>>,
    outputs: Collection<RangedOutput<*>>,
    val minTimeBetweenActions: Duration,
    expRepMaxSize: Int = 500_000,
    batchSize: Int = 32,
    targetDqnUpdateFreq: Int = 500,
    updateStart: Int = 10,
    rewardFactor: Double = 0.01,
    gamma: Double = 0.99,
    errorClamp: Double = 1.0,
    minEpsilon: Float = 0.00001f,
    epsilonNbStep: Int = 1_000,
    doubleDqn: Boolean = true
) : Output<T> where T : DaqcValue, T : Comparable<T> {

    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    //TODO
    private val trainingManagementDispatcher: CoroutineDispatcher = newSingleThreadContext("")

    private val environment = ControllerEnvironment(this)

    val targetInput = Recorder(
        targetInput,
        StorageFrequency.All,
        StorageSamples.Number(3)
    )
    val correlatedInputs = correlatedInputs.map {
        Recorder(it, StorageFrequency.All, StorageDuration.For(minTimeBetweenActions))
    }
    val outputs: List<Recorder<DaqcValue, RangedOutput<*>>> = kotlin.run {
        val outputsBuilder = ImmutableList.builder<Recorder<DaqcValue, RangedOutput<*>>>()
        outputsBuilder.addAll(outputs.map {
            Recorder(it, StorageFrequency.All, StorageDuration.For(minTimeBetweenActions))
        })
        outputsBuilder.build()
    }

    private val agent: OnlineQlDiscreteDense<Encodable>

    @Volatile
    private var agentRunner: Job? = null
        set(value) {
            field = value
            _isTransceiving.value = value != null
        }

    private val _isTransceiving = Updatable(false)
    override val isTransceiving get() = _isTransceiving

    private val _broadcastChannel = ConflatedBroadcastChannel<ValueInstant<T>>()

    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _broadcastChannel

    init {
        targetInput.startSampling()
        correlatedInputs.forEach {
            it.startSampling()
        }
        outputs.forEach {
            when (it) {
                is RangedQuantityOutput<*> -> it.setOutputToPercentMaximum(50.percent)
                is BinaryStateOutput -> it.setOutput(BinaryState.Low)
            }
        }

        val reinforcementConfig = QLearning.QLConfiguration(
            123, //Random seed
            Int.MAX_VALUE, //Max step By epoch
            Int.MAX_VALUE, //Max step
            expRepMaxSize, //Max size of experience replay
            batchSize, //size of batches
            targetDqnUpdateFreq, //target update (hard)
            updateStart, //num step noop warmup
            rewardFactor, //reward scaling (Reward discount factor I think)
            gamma, //gamma
            errorClamp, //td-error clipping
            minEpsilon, //min epsilon (why is this min and not just epsilon? maybe because it starts higher and comes down every epsilonNbStep)
            epsilonNbStep, //num step for eps greedy anneal (number of steps before reducing epsilon (reducing exploration))
            doubleDqn    //double DQN
        )

        val networkConfig: DQNFactoryStdDense.Configuration = DQNFactoryStdDense.Configuration.builder()
            .l2(0.001).updater(Adam(0.001)).numHiddenNodes(16).numLayer(3).build()

        agent = OnlineQlDiscreteDense(
            environment,
            networkConfig,
            reinforcementConfig,
            DataManager(false)
        ).apply {
            initMdp()
        }
    }

    override fun setOutput(setting: T): SettingViability.Viable {
        launch(trainingManagementDispatcher) {
            // Wait until all inputs have a value. This is hacky and sucks but rl4j makes life difficult.
            targetInput.updatable.startSampling()
            correlatedInputs.forEach {
                it.updatable.startSampling()
            }

            targetInput.updatable.getValue()
            correlatedInputs.forEach {
                it.updatable.getValue()
            }

            _broadcastChannel.send(setting.now())
            agentRunner = launch(trainingManagementDispatcher) {
                while (true) {
                    agent.trainStep(environment.getObservation())
                    delay(minTimeBetweenActions)
                }
            }
        }

        return SettingViability.Viable
    }

    override fun stopTransceiving() {
        agentRunner?.cancel()
        agentRunner = null
    }

    fun cancel() = job.cancel()

}