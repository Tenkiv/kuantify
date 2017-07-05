package com.tenkiv.daqc.recording

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.Instant

open class MemoryRecorder<T>(val samplesInMemory: Int = 10000,
                                             val fileName: String = "TempFile.json",
                                             val dataDeserializer: (String) -> T,
                                             val updatable: Updatable<ValueInstant<T>>) {

    private val fileWriter = BufferedWriter(FileWriter(fileName, true))
    private val jsonFactory = JsonFactory()
    private val jsonWriter = jsonFactory.createGenerator(fileWriter)
    private val jsonParser = jsonFactory.createParser(File(fileName))
    private val fileMutex = Mutex()

    private val VALUE = "value"
    private val TIME = "time"

    private val context = newSingleThreadContext("Recorder IO Context")

    private val currentBlock = ArrayList<ValueInstant<T>>()

    private var listenJob: Job? = null

    init {
        launch(context){ fileMutex.withLock { jsonWriter.writeStartArray() } }
        listenJob = updatable.openNewCoroutineListener(context,{cache(it)})
    }

    private suspend fun cache(value: ValueInstant<T>): Unit {
        if(currentBlock.size >= samplesInMemory){
            writeOut(currentBlock)
            currentBlock.clear()
        } else {
            currentBlock.add(value)
        }
    }

    private suspend fun writeOut(entry: List<ValueInstant<T>>) {
        fileMutex.withLock {
            entry.forEach {
                jsonWriter.writeStartObject()
                jsonWriter.writeStringField(VALUE, it.value.toString())
                jsonWriter.writeNumberField(TIME, it.instant.toEpochMilli())
                jsonWriter.writeEndObject()
            }
            fileWriter.flush()
        }
    }

    fun stop(){
        listenJob?.cancel()
        launch(context) {
            writeOut(currentBlock)
            fileMutex.withLock {
                jsonWriter.writeEndArray()
                jsonWriter.flush()
                jsonWriter.close()
            }
        }
    }

    fun getDataForTime(start: Instant, end: Instant): List<ValueInstant<T>>{
        val typedList = ArrayList<ValueInstant<T>>(
                currentBlock.filter { it.instant.isAfter(start) && it.instant.isBefore(end)})
        while (!(jsonParser.nextValue() == JsonToken.END_ARRAY &&
                jsonParser.currentName.isNullOrBlank()) && jsonParser.currentToken != null){
            if(jsonParser.currentToken() == JsonToken.START_ARRAY){
                var shouldTake: Boolean = false
                var lastInstant: Instant = Instant.now()
                    if(jsonParser.currentToken() == JsonToken.START_ARRAY) {
                        while (jsonParser.nextValue() != JsonToken.END_ARRAY) {
                            if (jsonParser.currentName != null && jsonParser.currentName == TIME) {
                                lastInstant = Instant.ofEpochMilli(jsonParser.valueAsLong)
                                if (lastInstant.isBefore(end) && lastInstant.isAfter(start)) {
                                    shouldTake = true
                                }
                            } else if (jsonParser.currentName != null && jsonParser.currentName == VALUE) {
                                if (shouldTake) {
                                    shouldTake = false
                                    val value = dataDeserializer(jsonParser.valueAsString)
                                    typedList.add(value.at(lastInstant))
                                }
                            }
                        }
                        return typedList
                    }
            }
        }
        return typedList
    }
}