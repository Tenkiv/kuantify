package org.tenkiv.daqc.learning.controller

import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import org.deeplearning4j.gym.StepReply
import org.deeplearning4j.rl4j.mdp.MDP
import org.deeplearning4j.rl4j.space.DiscreteSpace
import org.deeplearning4j.rl4j.space.ObservationSpace
import org.tenkiv.daqc.BinaryStateOutput
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.RangedQuantityOutput

class ControllerEnvironment<T>(private val controller: LearningController<T>) :
    MDP<ControllerObservation, Int, DiscreteSpace> where T : DaqcValue, T : Comparable<T> {

    val numQuantityOutputs = controller.outputs.count { it is RangedQuantityOutput<*> }

    val numBinaryStateOutputs = controller.outputs.count { it is BinaryStateOutput }

    private val actionHandlerList: List<OutputActionHandler> = kotlin.run {
        val listBuilder = ImmutableList.builder<OutputActionHandler>()

        controller.outputs.forEach {
            when (it) {
                is RangedQuantityOutput<*> -> listBuilder.add(QuantityOutputActionHandler(it))
                is BinaryStateOutput -> listBuilder.add(BinaryStateOutputActionHandler(it))
            }
        }

        listBuilder.build()
    }

    private val actionPermutationList: List<List<Int>> = Sets.cartesianProduct(
        actionHandlerList.map { it.actionSet }
    ).toList()

    override fun getActionSpace() = DiscreteSpace(actionPermutationList.size)

    override fun getObservationSpace(): ObservationSpace<ControllerObservation> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isDone(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun newInstance(): MDP<ControllerObservation, Int, DiscreteSpace> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reset(): ControllerObservation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun step(action: Int): StepReply<ControllerObservation> {

        actionPermutationList[action].forEachIndexed { index, individualAction ->
            actionHandlerList[index].takeAction(individualAction)
        }

        Thread.sleep(controller.minTimeBetweenActions.toMillis())

    }

    private fun getReward() {

    }

}