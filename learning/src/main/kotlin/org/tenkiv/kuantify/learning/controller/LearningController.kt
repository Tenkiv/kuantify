/*
 * Copyright 2018 Tenkiv, Inc.
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
 */

package org.tenkiv.kuantify.learning.controller

import com.google.common.collect.ImmutableList
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.deeplearning4j.rl4j.space.Encodable
import org.deeplearning4j.rl4j.util.DataManager
import org.nd4j.linalg.learning.config.Adam
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.now
import org.tenkiv.kuantify.data.BinaryState
import org.tenkiv.kuantify.data.DaqcValue
import org.tenkiv.kuantify.gate.acquire.input.RangedInput
import org.tenkiv.kuantify.gate.control.output.Output
import org.tenkiv.kuantify.gate.control.output.RangedOutput
import org.tenkiv.kuantify.gate.control.output.RangedQuantityOutput
import org.tenkiv.kuantify.gate.control.output.SettingResult
import org.tenkiv.kuantify.recording.RecordedUpdatable
import org.tenkiv.kuantify.recording.StorageDuration
import org.tenkiv.kuantify.recording.StorageFrequency
import org.tenkiv.kuantify.recording.StorageSamples
import org.tenkiv.kuantify.recording.binary.pairWithNewRecorder
import org.tenkiv.physikal.core.*
import java.time.Duration

//TODO: Make correlatedInputs optional, add overloads for optional binaryStateOutputs and quantityOutputs.
class LearningController<T>(
    targetInput: RangedInput<T>,
    correlatedInputs: Collection<RangedInput<*>>,
    binaryStateOutputs: Collection<RangedOutput<BinaryState>>,
    quantityOutputs: Collection<RangedQuantityOutput<*>>,
    val minTimeBetweenActions: Duration
) : Output<T> where T : DaqcValue, T : Comparable<T> {

    //TODO
    private val trainingManagementDispatcher: CoroutineDispatcher = newSingleThreadContext("")

    private val environment = ControllerEnvironment(this)

    val targetInput = targetInput.pairWithNewRecorder(StorageFrequency.All, StorageSamples.Number(3))
    val correlatedInputs = correlatedInputs.map {
        it.pairWithNewRecorder(StorageFrequency.All, StorageDuration.For(minTimeBetweenActions))
    }
    val outputs: List<RecordedUpdatable<DaqcValue, RangedOutput<*>>> = kotlin.run {
        val outputsBuilder = ImmutableList.builder<RecordedUpdatable<DaqcValue, RangedOutput<*>>>()
        outputsBuilder.addAll(binaryStateOutputs.map {
            it.pairWithNewRecorder(StorageFrequency.All, StorageDuration.For(minTimeBetweenActions))
        })
        outputsBuilder.addAll(quantityOutputs.map {
            it.pairWithNewRecorder(StorageFrequency.All, StorageDuration.For(minTimeBetweenActions))
        })
        outputsBuilder.build()
    }

    private val agent: QLearningDiscreteDense<Encodable>

    @Volatile
    override var isTransceiving = false

    private val _broadcastChannel = ConflatedBroadcastChannel<ValueInstant<T>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _broadcastChannel

    init {
        binaryStateOutputs.map {
            it.pairWithNewRecorder(StorageFrequency.All, StorageDuration.For(minTimeBetweenActions))
        }

        targetInput.startSampling()
        correlatedInputs.forEach {
            it.startSampling()
        }
        quantityOutputs.forEach {
            it.setOutputToPercentMaximum(50.percent)
        }
        binaryStateOutputs.forEach {
            it.setOutput(BinaryState.Off)
        }

        val reinforcementConfig = QLearning.QLConfiguration(
            123, //Random seed
            Int.MAX_VALUE, //Max step By epoch
            Int.MAX_VALUE, //Max step
            500000, //Max size of experience replay
            32, //size of batches
            500, //target update (hard)
            10, //num step noop warmup
            0.01, //reward scaling (Reward discount factor I think)
            0.99, //gamma
            1.0, //td-error clipping
            0.00001f, //min epsilon (why is this min and not just epsilon? maybe because it starts higher and comes down every epsilonNbStep)
            1000, //num step for eps greedy anneal (number of steps before reducing epsilon (reducing exploration))
            true    //double DQN
        )

        val networkConfig: DQNFactoryStdDense.Configuration = DQNFactoryStdDense.Configuration.builder()
            .l2(0.001).updater(Adam(0.001)).numHiddenNodes(16).numLayer(3).build()

        agent = QLearningDiscreteDense(environment, networkConfig, reinforcementConfig, DataManager(false))
    }

    override fun setOutput(setting: T, panicOnFailure: Boolean): SettingResult.Success {
        launch {
            // Wait until all inputs have a value. This is hacky and sucks but rl4j makes life difficult.
            targetInput.updatable.activate()
            correlatedInputs.forEach {
                it.updatable.activate()
            }

            targetInput.updatable.getValue()
            correlatedInputs.forEach {
                it.updatable.getValue()
            }

            _broadcastChannel.send(setting.now())
            launch(trainingManagementDispatcher) {
                agent.train()
            }
        }

        return SettingResult.Success
    }

    override fun stopTransceiving() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}