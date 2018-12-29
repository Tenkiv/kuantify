package org.tenkiv.kuantify.learning.lib

import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense
import org.deeplearning4j.rl4j.mdp.MDP
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.deeplearning4j.rl4j.space.DiscreteSpace
import org.deeplearning4j.rl4j.space.Encodable
import org.deeplearning4j.rl4j.util.DataManager

class OnlineQlDiscreteDense<O : Encodable>(
    mdp: MDP<O, Int, DiscreteSpace>,
    netConf: DQNFactoryStdDense.Configuration,
    conf: QLearning.QLConfiguration,
    dataManager: DataManager
) : QLearningDiscreteDense<O>(mdp, netConf, conf, dataManager) {

    public override fun trainStep(obs: O): QLStepReturn<O> = super.trainStep(obs)

}