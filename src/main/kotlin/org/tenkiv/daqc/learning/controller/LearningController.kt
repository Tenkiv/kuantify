package org.tenkiv.daqc.learning.controller

import com.google.common.collect.ImmutableList
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.deeplearning4j.rl4j.space.Encodable
import org.deeplearning4j.rl4j.util.DataManager
import org.nd4j.linalg.learning.config.Adam
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.now
import org.tenkiv.daqc.*
import org.tenkiv.daqc.lib.toPeriod
import org.tenkiv.daqc.recording.*
import org.tenkiv.physikal.core.times
import org.tenkiv.physikal.core.toDuration
import java.time.Duration
import kotlin.concurrent.thread

/**
 * Initialising this class is a blocking call.
 */
class LearningController<T>(
    targetInput: RangedInput<T>,
    correlatedInputs: Collection<RangedInput<*>>,
    binaryStateOutputs: Collection<RangedOutput<BinaryState>>,
    quantityOutputs: Collection<RangedQuantityOutput<*>>,
    val minTimeBetweenActions: Duration = targetInput.sampleRate.toPeriod().times(2).toDuration()
) : Output<T> where T : DaqcValue, T : Comparable<T> {

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
    override var isActive = false

    private val _broadcastChannel = ConflatedBroadcastChannel<ValueInstant<T>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _broadcastChannel

    init {
        binaryStateOutputs.map {
            it.pairWithNewRecorder(StorageFrequency.All, StorageDuration.For(minTimeBetweenActions))
        }

        targetInput.activate()
        correlatedInputs.forEach {
            it.activate()
        }
        quantityOutputs.forEach {
            val middle = (it.valueRange.start.toDoubleInSystemUnit() +
                    it.valueRange.endInclusive.toDoubleInSystemUnit()) / 2
            it.setOutputInSystemUnit(middle)
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

    override fun setOutput(setting: T) {
        launch {
            // Wait until all inputs have a value. This is hacky and sucks but rl4j makes life difficult.
            targetInput.updatable.getValue()

            correlatedInputs.forEach {
                it.updatable.getValue()
            }

            _broadcastChannel.send(setting.now())
            thread {
                agent.train()
            }
        }
    }

    override fun deactivate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}