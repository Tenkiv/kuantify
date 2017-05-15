package com.tenkiv.daqc.recording

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import tec.uom.se.unit.Units
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.measure.quantity.Time

/**
 * Created by tenkiv on 5/15/17.
 */
abstract class Recorder(val timeToRecord: Time? = null, vararg recordingObjects: RecordingObject): Updatable<DaqcValue.Boolean> {

    override val listeners: MutableList<UpdatableListener<DaqcValue.Boolean>> = CopyOnWriteArrayList()

    private var _value: DaqcValue.Boolean? = null

    override var value: DaqcValue.Boolean?
        get() = _value
        set(value) { _value = value; listeners.forEach{ it.onUpdate(this) } }

    protected val recArray = recordingObjects

    protected abstract val onDataUpdate: UpdatableListener<DaqcValue>

    open fun start(){ recArray.forEach {
        if(timeToRecord != null){
            async(CommonPool){
                delay(timeToRecord.to(Units.SECOND).value.toLong(),TimeUnit.SECONDS)
                stop()
            }
        }

        it.updatable.listeners.add(onDataUpdate) }
        value = DaqcValue.Boolean(true)
    }

    open fun stop(){
        recArray.forEach { it.updatable.listeners.remove(onDataUpdate) }
        value = DaqcValue.Boolean(false)
    }
}

data class RecordingObject(val name: String, val updatable: Updatable<DaqcValue>)