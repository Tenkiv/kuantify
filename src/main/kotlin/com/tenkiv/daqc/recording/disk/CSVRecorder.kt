package com.tenkiv.daqc.recording.disk

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.recording.Recorder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.io.FileOutputStream
import java.time.Instant
import javax.measure.quantity.Time

/**
 * Created by tenkiv on 4/11/17.
 */
class CSVRecorder(path: String,
                  val numberOfSamples: Int = -1,
                  timeToRecord: Time? = null,
                  recordingObjects: Map<Updatable<DaqcValue>, String>) : Recorder<DaqcValue>(timeToRecord, recordingObjects) {

    override val broadcastChannel = ConflatedBroadcastChannel<BinaryState>()

    val outputStream = FileOutputStream(path, true)

    val csvContext = newSingleThreadContext("CSV Recorder Context")

    var isFirstWrite = true

    private val csvMutex = Mutex()

    var sampleTally = 0

    override fun stop() {
        super.stop()
        launch(csvContext){
            csvMutex.withLock {
                outputStream.flush()
                outputStream.close()
            }
        }
    }

    suspend override fun onUpdate(updatable: Updatable<DaqcValue>, value: DaqcValue) {

        println("Value has been gotten $value")

        fun writeValue(value: String) {
            launch(csvContext) {
                csvMutex.withLock {
                    outputStream.use { it.write("$value".toByteArray()) }
                }
            }
        }

        if (isFirstWrite) {
            recordingObjects.values.forEach{writeValue("$it,")}
            writeValue("Time\n")
            isFirstWrite = false
        }

        recordingObjects.keys.forEach { writeValue("$value,") }
        writeValue("${Instant.now().epochSecond}\n")

        sampleTally++

        if (sampleTally == numberOfSamples) {
            stop()
        }
    }
}