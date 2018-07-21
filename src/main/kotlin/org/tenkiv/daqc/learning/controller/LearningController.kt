package org.tenkiv.daqc.learning.controller

import com.google.common.collect.ImmutableList
import kotlinx.coroutines.experimental.future.asCompletableFuture
import org.tenkiv.daqc.*
import org.tenkiv.daqc.lib.toDuration
import org.tenkiv.daqc.lib.toPeriod
import org.tenkiv.daqc.recording.*
import org.tenkiv.physikal.core.times
import java.time.Duration

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

    init {
        binaryStateOutputs.map {
            it.pairWithNewRecorder(StorageFrequency.All, StorageDuration.For(minTimeBetweenActions))
        }

        targetInput.activate()
        correlatedInputs.forEach {
            it.activate()
        }
        // Wait until all inputs have a value. This is hacky and sucks but rl4j makes life difficult.
        targetInput.value.asCompletableFuture().join()
        correlatedInputs.forEach {
            it.value.asCompletableFuture().join()
        }
        quantityOutputs.forEach {
            val middle = (it.valueRange.start.toDoubleInSystemUnit() +
                    it.valueRange.endInclusive.toDoubleInSystemUnit()) / 2
            it.setOutputInSystemUnit(middle)
        }
        binaryStateOutputs.forEach {
            it.setOutput(BinaryState.Off)
        }
    }

}