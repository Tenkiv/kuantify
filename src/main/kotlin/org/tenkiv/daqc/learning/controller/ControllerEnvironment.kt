package org.tenkiv.daqc.learning.controller

import com.google.common.collect.ImmutableList
import org.deeplearning4j.gym.StepReply
import org.deeplearning4j.rl4j.mdp.MDP
import org.deeplearning4j.rl4j.space.DiscreteSpace
import org.deeplearning4j.rl4j.space.ObservationSpace
import org.tenkiv.daqc.BinaryStateOutput
import org.tenkiv.daqc.RangedQuantityOutput

class ControllerEnvironment(private val controller: LearningController<*>) :
    MDP<ControllerObservation, Int, DiscreteSpace> {

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

    override fun getActionSpace() = DiscreteSpace()

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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getDiscreteSpaceSize() {


    }

}