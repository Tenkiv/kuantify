package com.tenkiv.daqc.recording

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import java.io.FileOutputStream
import java.time.Instant
import javax.measure.quantity.Time

/**
 * Created by tenkiv on 4/11/17.
 */
class CSVRecorder(path: String, numberOfSamples: Int = -1,
                  timeToRecord: Time?,
                  vararg recordingObjects: RecordingObject): Recorder(timeToRecord, *recordingObjects) {

    val outputStream = FileOutputStream(path,true)

    var isFirstWrite = true

    var sampleTally = 0

    override val onDataUpdate = object: UpdatableListener<DaqcValue> {
        override fun onUpdate(updatedObject: Updatable<DaqcValue>) {

            fun writeValue(value: String){
                outputStream.use {
                    it.write("$value,".toByteArray())
                }
            }

            if(isFirstWrite){
                recordingObjects.forEach { writeValue(it.name) }
                outputStream.use { it.write("TIME\n".toByteArray()) }
            }

            recordingObjects.forEach { writeValue(it.updatable.value.toString()) }
            outputStream.use { it.write("${Instant.now().epochSecond}\n".toByteArray()) }

            sampleTally++

            if(sampleTally == numberOfSamples){
                stop()
            }
        }
    }
}