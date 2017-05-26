package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.*
import com.tenkiv.daqc.hardware.definitions.BasicUpdatable
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch

/**
 * Created by tenkiv on 4/17/17.
 */
abstract class Sensor<O: DaqcValue>(inputs: Collection<Input<O>>): Input<O>, UpdatableListener<O>, BasicUpdatable<O>() {

    override val openSubChannels: MutableList<SubscriptionReceiveChannel<Updatable<O>>> = ArrayList()

    init { launch(context){ inputs.forEach { openSubChannels.add(it.broadcastChannel.consumeAndReturn{onDataReceived(it)}) } } }

    open fun addTrigger(condition: (O) -> Boolean, function: () -> Unit): Trigger<O>
        { return Trigger(TriggerCondition(this, condition) , triggerFunction = function) }

}