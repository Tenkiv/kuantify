package com.tenkiv.daqc.recording

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import java.io.FileOutputStream
import java.time.Instant

/**
 * Created by tenkiv on 4/11/17.
 */
class CSVRecorder( path: String,
                  val numberOfSamples: Int = -1,
                  vararg csvValues: Updatable<DaqcValue>){

    val csvVal: Array<out Updatable<DaqcValue>> = csvValues

    val outputStream = FileOutputStream(path,true)

    var isFirstWrite = true

    var sampleTally = 0


    init{
        csvVal.forEach { it.listeners.add(onDataUpdate) }
    }

    private val onDataUpdate = object: UpdatableListener<DaqcValue> {
        override fun onUpdate(data: Updatable<DaqcValue>) {

            fun writeValue(value: String){
                outputStream.use {
                    it.write("$value,".toByteArray())
                }
            }

            if(isFirstWrite){
                csvVal.forEach { writeValue(it.toString()) }
                outputStream.use { it.write("TIME\n".toByteArray()) }
            }

            csvVal.forEach { writeValue(it.value.toString()) }
            outputStream.use { it.write("${Instant.now().epochSecond}\n".toByteArray()) }

            sampleTally++

            if(sampleTally == numberOfSamples){
                stop()
            }
        }
    }

    fun stop(){
        csvVal.forEach { it.listeners.remove(onDataUpdate) }
    }
}