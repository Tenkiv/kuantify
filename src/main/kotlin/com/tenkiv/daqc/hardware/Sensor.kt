package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.Trigger
import com.tenkiv.daqc.TriggerCondition
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.channel.Input
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by tenkiv on 4/17/17.
 */
abstract class Sensor<O: DaqcValue>(inputs: Collection<Input<DaqcValue>>): Input<O> {

    override val listeners: MutableList<UpdatableListener<O>> = CopyOnWriteArrayList()

    private var _value: O? = null

    override var value: O?
        get() = _value
        set(value) { _value = value; listeners.forEach{ it.onUpdate(this) } }

    init { inputs.forEach {it.listeners.add(onDataReceived) } }

    abstract val onDataReceived: UpdatableListener<DaqcValue>

    open fun addTrigger(condition: (O) -> Boolean, function: () -> Unit): Trigger<O>
        { return Trigger(TriggerCondition(this, condition) , triggerFunction = function) }

}