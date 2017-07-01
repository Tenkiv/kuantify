package com.tenkiv.daqc.recording.disk

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.recording.Recorder
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.io.BufferedWriter
import java.io.FileWriter
import javax.measure.quantity.Time


abstract class Writer<T : DaqcValue>(val path: String,
                                     timeToRecord: Time?,
                                     recordingObjects: Map<Updatable<T>, String>) : Recorder<T>(timeToRecord, recordingObjects) {

    protected val fileWriter = BufferedWriter(FileWriter(path, true))

    protected val filePathContext = newSingleThreadContext("File Context $path")

    open fun write(output: String) {
        launch(filePathContext) {
            fileWriter.append(output)
            fileWriter.flush()
        }
    }
}