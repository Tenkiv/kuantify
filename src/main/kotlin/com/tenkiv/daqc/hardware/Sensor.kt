package com.tenkiv.daqc.hardware

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.Trigger
import com.tenkiv.daqc.TriggerCondition
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch

/**
 * Created by tenkiv on 4/17/17.
 */
abstract class Sensor<O : DaqcValue, R: DaqcValue>(inputs: Collection<Input<O>>) : Input<R>, Updatable<R>, UpdatableListener<O> {

    constructor(input: Input<O>): this(listOf<Input<O>>(input))

    init {
        launch(DAQC_CONTEXT) {
            inputs.forEach { input -> input.broadcastChannel.consumeEach { value -> onUpdate(input, value) } }
        }
    }

    override val broadcastChannel: ConflatedBroadcastChannel<R> = ConflatedBroadcastChannel()

    open fun addTrigger(condition: (R) -> Boolean, function: () -> Unit): Trigger<R> {
        return Trigger(TriggerCondition(this, condition), triggerFunction = function)
    }

}