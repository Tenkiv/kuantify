package com.tenkiv.daqc.recording

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import java.io.*
import java.time.Instant

open class MemoryRecorder<T>(val samplesInMemory: Int = 10,
                                             val fileName: String = "TempFile.json",
                                             val dataDeserializer: (String) -> T,
                                             val updatable: Updatable<ValueInstant<T>>) {

    init { updatable.openNewCoroutineListener(DAQC_CONTEXT,{cache(it)}) }

    private val fileWriter = BufferedWriter(FileWriter(fileName, true))
    private val jsonFactory = JsonFactory()
    private val jsonWriter = jsonFactory.createGenerator(fileWriter)
    private val jsonParser = jsonFactory.createParser(File(fileName))

    private val VALUE = "value"
    private val TIME = "time"

    private val currentBlock = ArrayList<ValueInstant<T>>()

    private suspend fun cache(value: ValueInstant<T>): Unit {
        if(currentBlock.size >= samplesInMemory){
            writeOut(currentBlock)
            currentBlock.clear()
        } else {
            currentBlock.add(value)
        }
    }

    private fun writeOut(entry: List<ValueInstant<T>>){
        entry.forEach {
            jsonWriter.writeStartObject()
            jsonWriter.writeStringField(VALUE,it.value.toString())
            jsonWriter.writeNumberField(TIME,it.instant.toEpochMilli())
            jsonWriter.writeEndObject()
        }
        fileWriter.flush()
    }

    fun getDataForTime(start: Instant, end: Instant): List<ValueInstant<T>>{
        val typedList = ArrayList<ValueInstant<T>>(
                currentBlock.filter { it.instant.isAfter(start) && it.instant.isBefore(end)})

        while (!(jsonParser.nextValue() == JsonToken.END_ARRAY && jsonParser.currentName.isNullOrBlank())){
            if(jsonParser.currentToken() == JsonToken.START_ARRAY){
                var shouldTake: Boolean = false
                var lastInstant: Instant = Instant.now()
                while (jsonParser.nextValue() != JsonToken.END_ARRAY) {
                    if(jsonParser.currentName != null && jsonParser.currentName == TIME) {
                        lastInstant = Instant.ofEpochMilli(jsonParser.valueAsLong)
                        if(lastInstant.isBefore(end) && lastInstant.isAfter(start)){
                            shouldTake = true
                        }
                    }else if(jsonParser.currentName != null && jsonParser.currentName == VALUE) {
                        if(shouldTake){
                            shouldTake = false
                            val value = dataDeserializer(jsonParser.valueAsString)
                            typedList.add(value.at(lastInstant))
                        }
                    }
                }
            }
        }
        return typedList
    }
}