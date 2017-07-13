package com.tenkiv.daqc.recording

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqcThreadContext
import com.tenkiv.getMemoryRecorderUid
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.nio.aWrite
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.coral.secondsSpan
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

fun <T> recorder(storageFrequency: StorageFrequency = StorageFrequency.All,
                 memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
                 diskDuration: StorageDuration = StorageDuration.Forever,
                 dataDeserializer: (String) -> T,
                 updatable: Updatable<ValueInstant<T>>) = Recorder(storageFrequency,
        memoryDuration,
        diskDuration,
        dataDeserializer,
        updatable)

fun <T : DaqcValue> daqcValueRecorder(storageFrequency: StorageFrequency = StorageFrequency.All,
                                      memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
                                      diskDuration: StorageDuration = StorageDuration.Forever,
                                      updatable: Updatable<ValueInstant<T>>) = Recorder(storageFrequency,
        memoryDuration,
        diskDuration,
        { TODO("Use jackson or parsing here") },
        updatable)

fun <T> Updatable<ValueInstant<T>>.getRecorder(storageFrequency: StorageFrequency = StorageFrequency.All,
                                               memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
                                               diskDuration: StorageDuration = StorageDuration.Forever,
                                               dataDeserializer: (String) -> T): Recorder<T> = Recorder(storageFrequency,
        memoryDuration,
        diskDuration,
        dataDeserializer,
        this)


open class Recorder<T> internal constructor(val storageFrequency: StorageFrequency = StorageFrequency.All,
                                            val memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
                                            val diskDuration: StorageDuration = StorageDuration.Forever,
                                            val dataDeserializer: (String) -> T,
                                            val updatable: Updatable<ValueInstant<T>>) {

    val uid = getMemoryRecorderUid()

    private val currentFilesMap = HashMap<Instant,File>()

    private var jsonFactory = JsonFactory()

    //private var fileWriter: JsonGenerator = getNewFileWriter()

    private val fileChannel: AsynchronousFileChannel

    private var filePosition = 0L

    //private val jobQueue = LinkedBlockingQueue<Pair<RecorderAction,() -> Unit>>()

    //private val writeLock = ReentrantReadWriteLock()

    //private val jobLock = ReentrantReadWriteLock()

    private val executor = Executors.newSingleThreadExecutor()

    private var currentInterval: Long = 0L

    private val VALUE = "value"
    private val TIME = "time"

    private fun getNewFileWriter(): AsynchronousFileChannel {
        val now = Instant.now()
        val file = File("tempStorage_${uid}_${now.toEpochMilli()}.json")
        val writer = AsynchronousFileChannel.open(file.toPath())

        //writeLock.write {
        launch(daqcThreadContext) {
            //TODO writeJsonBuffer("[")
        }
        if (currentFilesMap.size != 0) {
            if (fileChannel.isOpen) {
                try {
                    launch(daqcThreadContext) {
                        //TODO writeJsonBuffer("]")
                        fileChannel.force(true)
                        fileChannel.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            }
        //}
        currentFilesMap.put(now,file)
        return writer
    }

    private val dataInMemory = ArrayList<ValueInstant<T>>()

    private var listenJob: Job

    private val diskTimer = Timer()
    private val diskExpirationTask: TimerTask = object : TimerTask() {
        override fun run() {
            //TODO fileChannel = getNewFileWriter()
            checkFileAge()
        }
    }

    private val memoryTimer = Timer()
    private val memoryExpirationTask: TimerTask = object : TimerTask() {
        override fun run() {
            checkMemoryAge()
        }
    }

    init {
        fileChannel = getNewFileWriter()

        if (diskDuration is StorageDuration.For) {
            val delay = diskDuration.duration.toMillis() / 2
            diskTimer.scheduleAtFixedRate(diskExpirationTask, delay, delay)
        }

        if (memoryDuration is StorageDuration.For) {
            val delay = memoryDuration.duration.toMillis() / 2
            memoryTimer.scheduleAtFixedRate(memoryExpirationTask, delay, delay)
        }

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

    private fun cachePerNumMeasurement(value: ValueInstant<T>, samples: Int) {
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
                if (Duration.between(it.key, now) > diskDuration.duration) {
                    launch(daqcThreadContext) {
                        currentFilesMap[it.key]?.delete()
                        currentFilesMap.remove(it.key)
                    }
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

    private fun pollJobs() {
        /*if(jobQueue.size != 0){
            val currentAction = jobQueue.poll()
                jobLock.write { currentAction.second.invoke() }

        }*///TODO()
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

    private fun cacheAll(value: ValueInstant<T>) {
        dataInMemory.add(value)
        if(memoryDuration == StorageDuration.Forever && diskDuration != StorageDuration.Never){
            writeOut(value)
        }
    }

    private fun writeOut(entry: List<ValueInstant<T>>) {
        if(diskDuration != StorageDuration.Never) {
            entry.forEach { writeOut(it) }
        }
    }

    private fun writeOut(entry: ValueInstant<T>) {
        if(diskDuration != StorageDuration.Never) {
            launch(daqcThreadContext) {
                //TODO writeJsonBuffer("{$TIME:${entry.instant.toEpochMilli()},$VALUE:${entry.value}}")
            }
        }
    }

    suspend fun writeJsonBuffer(string: String,
                                fileChannel: AsynchronousFileChannel,
                                filePosition: Long,
                                charset: Charset = Charset.defaultCharset()): Long {
        val buffer = ByteBuffer.wrap(string.toByteArray(charset))
        fileChannel.aWrite(buffer, filePosition)
        return (filePosition + string.length)
    }

    fun stop(){
        diskExpirationTask.cancel()
        listenJob.cancel()
        writeOut(dataInMemory)
        launch(daqcThreadContext) {
            //TODO writeJsonBuffer("]")
            fileChannel.force(true)
            fileChannel.close()
        }
    }

    fun getDataInMemory(): List<ValueInstant<T>> = dataInMemory

    fun getDataForTime(start: Instant, end: Instant): Future<List<ValueInstant<T>>> {
        val future: Future<List<ValueInstant<T>>> = executor.submit(Callable<List<ValueInstant<T>>> {
            val found = ArrayList<ValueInstant<T>>(
                    dataInMemory.filter { it.instant.isAfter(start) && it.instant.isBefore(end) })
            if(!(dataInMemory.filter { it.instant.isAfter(end) }.isNotEmpty())) {
                currentFilesMap.forEach {
                    try {
                        val jsonParser = jsonFactory.createParser(it.value)
                        while (!(jsonParser.nextValue() == JsonToken.END_ARRAY &&
                                jsonParser.currentName.isNullOrBlank()) && jsonParser.currentToken != null) {
                            if (jsonParser.currentToken() == JsonToken.START_ARRAY) {
                                var shouldTake: Boolean = false
                                var lastInstant: Instant = Instant.now()
                                if (jsonParser.currentToken() == JsonToken.START_ARRAY) {
                                    while (true) {
                                        try {
                                            if (jsonParser.nextValue() == JsonToken.END_ARRAY) {
                                                break
                                            }
                                        } catch (endOfFileException: IOException) {
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
                                    return@Callable found
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return@Callable found
        })

        return future
    }
}