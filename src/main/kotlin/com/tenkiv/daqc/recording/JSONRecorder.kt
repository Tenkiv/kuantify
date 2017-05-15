package com.tenkiv.daqc.recording

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import java.time.Instant
import javax.measure.quantity.Time

/**
 * Created by tenkiv on 5/15/17.
 */
class JSONRecorder(path: String,
                   jsonArraySize: Int = 1000,
                   timeToRecord: Time? = null,
                   recordingObjects: Map<Updatable<DaqcValue>,String>) : Writer(path, timeToRecord, recordingObjects) {

    private var subJsonArray = JsonArray(emptyList<JsonObject>())

    private var completeArray = JsonArray(emptyList<JsonArray<JsonObject>>())

    override val onDataUpdate = object: UpdatableListener<DaqcValue>{
        override fun onUpdate(updatedObject: Updatable<DaqcValue>) {

            println("Object Updated: ${updatedObject.value}")

            val jsonObj = mapOf(Pair(recordingObjects[updatedObject] ?: "null", updatedObject.value.toString()),
                                Pair("Time",Instant.now().epochSecond))

            subJsonArray.add(JsonObject(jsonObj))

            if(jsonArraySize <= subJsonArray.size){
                writeOutJSON()
            }
        }
    }

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