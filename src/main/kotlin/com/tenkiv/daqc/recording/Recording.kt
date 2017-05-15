package com.tenkiv.daqc.recording

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.*
import tec.uom.se.unit.Units
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.measure.quantity.Time

/**
 * Created by tenkiv on 5/15/17.
 */
abstract class Recorder(val timeToRecord: Time? = null,
                        val recordingObjects: Map<Updatable<DaqcValue>,String>): Updatable<DaqcValue.Boolean> {

    override val listeners: MutableList<UpdatableListener<DaqcValue.Boolean>> = CopyOnWriteArrayList()

    private var _value: DaqcValue.Boolean? = null

    override var value: DaqcValue.Boolean?
        get() = _value
        set(value) { _value = value; listeners.forEach{ it.onUpdate(this) } }


    protected abstract val onDataUpdate: UpdatableListener<DaqcValue>

    open fun start(){

        if(timeToRecord != null){
            async(CommonPool){
                delay(timeToRecord.to(Units.SECOND).value.toLong(),TimeUnit.SECONDS)
                stop()
            }
        }

        recordingObjects.keys.forEach { it.listeners.add(onDataUpdate) }

        value = DaqcValue.Boolean(true)
    }

    open fun stop(){
        recordingObjects.keys.forEach { it.listeners.remove(onDataUpdate) }
        value = DaqcValue.Boolean(false)
    }
}

abstract class Writer(val path: String,
                      timeToRecord: Time?,
                      recordingObjects: Map<Updatable<DaqcValue>,String>): Recorder(timeToRecord, recordingObjects) {

    protected val fileWriter = BufferedWriter(FileWriter(path, true))

    protected val filePathContext = newSingleThreadContext("File Context $path")

    open fun write(output: String){
        launch(filePathContext){
            fileWriter.append(output)
            fileWriter.flush()
        }
    }
}

data class RecordingObject(val name: String, val updatable: Updatable<DaqcValue>)

fun MutableList<DaqcValue>.addValue(value: DaqcValue.Boolean){
    this.add(value)
}

fun MutableList<DaqcValue>.addValue(value: DaqcValue.Quantity<*>){

}