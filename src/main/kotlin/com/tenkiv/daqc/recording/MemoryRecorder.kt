package com.tenkiv.daqc.recording

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import com.tenkiv.daqc.RecorderAction
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqcThreadContext
import com.tenkiv.getMemoryRecorderUid
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ArrayChannel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.LinkedListChannel
import kotlinx.coroutines.experimental.sync.Mutex
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.coral.isOlderThan
import org.tenkiv.coral.secondsSpan
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class MemoryRecorder<T>(val storageFrequency: StorageFrequency = StorageFrequency.All,
                             val memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
                             val diskDuration: StorageDuration = StorageDuration.Forever,
                             val dataDeserializer: (String) -> T,
                             val updatable: Updatable<ValueInstant<T>>) {

    val uid = getMemoryRecorderUid()

    private val currentFilesMap = HashMap<Instant,File>()

    private var jsonFactory = JsonFactory()

    private var fileWriter: JsonGenerator = getNewFileWriter()

    private var directory: File = File("").absoluteFile

    private val oldestFileMutex = Mutex()

    private val jobList = ArrayList<Pair<RecorderAction,Job>>()

    private var currentInterval: Long = 0L

    private val VALUE = "value"
    private val TIME = "time"

    private fun getNewFileWriter(): JsonGenerator{
        val now = Instant.now()
        val file = File("tempStorage_${uid}_${now.toEpochMilli()}.json")
        val writer = jsonFactory.createGenerator(FileWriter(file))
        if(currentFilesMap.size != 0){
            launch(daqcThreadContext) {
                fileWriter.writeEndArray()
                fileWriter.close()
            }
        }
        currentFilesMap.put(now,file)
        return writer
    }

    private val dataInMemory = ArrayList<ValueInstant<T>>()

    private var listenJob: Job

    private var expirationJob: Job? = null

    init {

        if (diskDuration is StorageDuration.For) {
            expirationJob = launch(daqcThreadContext) {
                val delay = diskDuration.duration.toMillis() / 2
                while (true) {
                    delay(delay, TimeUnit.MILLISECONDS)
                    fileWriter = getNewFileWriter()
                    checkFileAge()
                }
            }
        }

        if (memoryDuration is StorageDuration.For) {
            expirationJob = launch(daqcThreadContext) {
                val delay = memoryDuration.duration.toMillis() / 2
                while (true) {
                    delay(delay, TimeUnit.MILLISECONDS)
                    checkMemoryAge()
                }
            }
        }

        fileWriter.writeStartArray()
        if(memoryDuration != StorageDuration.Never) {
            listenJob = updatable.openNewCoroutineListener {
                        when (storageFrequency) {
                            is StorageFrequency.PerNumMeasurements -> {
                                cachePerNumMeasurement(it, storageFrequency.number)
                            }
                            is StorageFrequency.Interval -> {
                                cachePerInterval(it, storageFrequency.interval)
                            }
                            is StorageFrequency.All -> {
                                cacheAll(it)
                            }
                        }
                    }
        }else{
            listenJob = updatable.openNewCoroutineListener {
                writeOut(it)
            }
        }
    }

    private suspend fun cachePerNumMeasurement(value: ValueInstant<T>, samples: Int) {
        if(dataInMemory.size >= samples){
            writeOut(dataInMemory)
            dataInMemory.clear()
        } else {
            dataInMemory.add(value)
        }
    }

    private fun checkFileAge(){
        if(diskDuration is StorageDuration.For) {
            val now = Instant.now()
            currentFilesMap.iterator().forEach {
                if (Duration.between(it.key,now)>diskDuration.duration){
                    val job = launch(daqcThreadContext,CoroutineStart.LAZY){ currentFilesMap[it.key]?.delete() }
                    job.invokeOnCompletion { launch(daqcThreadContext){ pollJobs() } }

                    jobList.add(Pair(RecorderAction.DELETE, job))
                }
            }
        }
    }

    private fun checkMemoryAge(){
        val now = Instant.now()
        if(memoryDuration is StorageDuration.For){
            dataInMemory.iterator().forEach {
                launch(daqcThreadContext) {
                    writeOut(it)
                    dataInMemory.remove(it)
                }
            }
        }
    }

    private suspend fun pollJobs(){
        jobList.removeIf { it.second.isCompleted }
        if(jobList.size != 0){
            val currentAction: RecorderAction = jobList[0].first
            for((action,job) in jobList) {
                if(action == currentAction && !job.isActive && !job.isCompleted){
                    job.start()
                }else if(action != currentAction){
                    break
                }
            }
        }
    }

    private fun checkInterval(duration: Duration): Boolean{
        val potential = (Instant.now().toEpochMilli()/duration.toMillis())
        if(currentInterval - potential < 0){
            currentInterval = potential
            return true
        }
        return false
    }

    private suspend fun cachePerInterval(value: ValueInstant<T>, duration: Duration) {
        if(checkInterval(duration)){
            writeOut(dataInMemory)
            dataInMemory.clear()
        } else {
            dataInMemory.add(value)
        }
    }

    private suspend fun cacheAll(value: ValueInstant<T>){
        dataInMemory.add(value)
        if(memoryDuration == StorageDuration.Forever && diskDuration != StorageDuration.Never){
            writeOut(value)
        }
    }

    private suspend fun writeOut(entry: List<ValueInstant<T>>) {
        if(diskDuration != StorageDuration.Never) {
            entry.forEach { writeOut(it) }
        }
    }

    private suspend fun writeOut(entry: ValueInstant<T>) {
        if(diskDuration != StorageDuration.Never) {
            fileWriter.writeStartObject()
            fileWriter.writeNumberField(TIME, entry.instant.toEpochMilli())
            fileWriter.writeStringField(VALUE, entry.value.toString())
            fileWriter.writeEndObject()
            fileWriter.flush()
        }
    }

    fun stop(){
        expirationJob?.cancel()
        listenJob.cancel()
        launch(daqcThreadContext) {
            writeOut(dataInMemory)
            fileWriter.writeEndArray()
            fileWriter.flush()
            fileWriter.close()
        }
    }

    fun getDataInMemory(): List<ValueInstant<T>> = dataInMemory

    fun getDataForTime(start: Instant, end: Instant): LinkedListChannel<ValueInstant<T>> {
        val channel = LinkedListChannel<ValueInstant<T>>()
        val job = launch(daqcThreadContext, CoroutineStart.LAZY){
            val found = ArrayList<ValueInstant<T>>(
                    dataInMemory.filter { it.instant.isAfter(start) && it.instant.isBefore(end) })
            //If its not pulling from memory its this line.
            if(!(dataInMemory.filter { it.instant.isAfter(end) }.isNotEmpty())) {
                currentFilesMap.forEach {
                    val jsonParser = jsonFactory.createParser(it.value)
                    while (!(jsonParser.nextValue() == JsonToken.END_ARRAY &&
                            jsonParser.currentName.isNullOrBlank()) && jsonParser.currentToken != null) {
                        if (jsonParser.currentToken() == JsonToken.START_ARRAY) {
                            var shouldTake: Boolean = false
                            var lastInstant: Instant = Instant.now()
                            if (jsonParser.currentToken() == JsonToken.START_ARRAY) {
                                while (true) {
                                    try {
                                        if(jsonParser.nextValue() == JsonToken.END_ARRAY){ break }
                                    }catch (endOfFileException: IOException){
                                        //File isn't finished being written to. Break if fail
                                        break
                                    }
                                    if (jsonParser.currentName != null && jsonParser.currentName == TIME) {
                                        lastInstant = Instant.ofEpochMilli(jsonParser.valueAsLong)
                                        if (lastInstant.isBefore(end) && lastInstant.isAfter(start)) {
                                            shouldTake = true
                                        }
                                    } else if (jsonParser.currentName != null && jsonParser.currentName == VALUE) {
                                        if (shouldTake) {
                                            shouldTake = false
                                            val value = dataDeserializer(jsonParser.valueAsString)
                                            found.add(value.at(lastInstant))
                                        }
                                    }
                                }
                                return@forEach
                            }
                        }
                    }
                }
            }
            try {
                found.forEach { channel.send(it) }
            }finally {
                channel.close()
            }
        }
        job.invokeOnCompletion { launch(daqcThreadContext){ pollJobs() } }
        jobList.add(Pair(RecorderAction.SEARCH,job))

        try {
            return channel
        } finally {
            launch(daqcThreadContext){ pollJobs() }
        }
    }
}