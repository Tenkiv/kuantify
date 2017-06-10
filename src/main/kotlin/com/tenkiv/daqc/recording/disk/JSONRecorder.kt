package com.tenkiv.daqc.recording.disk

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import java.time.Instant
import javax.measure.quantity.Time

/**
 * Created by tenkiv on 5/15/17.
 */
class JSONRecorder(path: String,
                   val jsonArraySize: Int = 1000,
                   timeToRecord: Time? = null,
                   recordingObjects: Map<Updatable<DaqcValue>,String>) : Writer<DaqcValue>(path, timeToRecord, recordingObjects) {

    override val broadcastChannel = ConflatedBroadcastChannel<DaqcValue.Boolean>()

    suspend override fun onUpdate(updatable: Updatable<DaqcValue>, value: DaqcValue) {
        val jsonObj = mapOf(Pair(recordingObjects[updatable] ?: "null", value.toString()),
                Pair("time", Instant.now().epochSecond))

        subJsonArray.add(JsonObject(jsonObj))

        if(jsonArraySize <= subJsonArray.size){
            writeOutJSON()
        }
    }

    private var subJsonArray = JsonArray(emptyList<JsonObject>())

    private var completeArray = JsonArray(emptyList<JsonArray<JsonObject>>())

    override fun stop() {
        super.stop()
        writeOutJSON()
    }

    private fun writeOutJSON(){
        completeArray.add(subJsonArray)
        subJsonArray = JsonArray(emptyList<JsonObject>())
        write(completeArray.toJsonString())
    }
}