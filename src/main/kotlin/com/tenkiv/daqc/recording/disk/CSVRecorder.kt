package com.tenkiv.daqc.recording.disk

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.recording.Recorder
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
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

    var isFirstWrite = true

    var sampleTally = 0

    suspend override fun onUpdate(updatable: Updatable<DaqcValue>, value: DaqcValue) {
        fun writeValue(value: String) {
            outputStream.use {
                it.write("$value,".toByteArray())
            }
        }

        if (isFirstWrite) {
            recordingObjects.values.forEach(::writeValue)
            outputStream.use { it.write("TIME\n".toByteArray()) }
        }

        recordingObjects.keys.forEach { writeValue(value.toString()) }
        outputStream.use { it.write("${Instant.now().epochSecond}\n".toByteArray()) }

        sampleTally++

        if (sampleTally == numberOfSamples) {
            stop()
        }
    }
}