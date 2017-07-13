package org.tenkiv.daqc.recording

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.nio.aWrite
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqcThreadContext
import org.tenkiv.getMemoryRecorderUid
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
import kotlin.collections.ArrayList


open class Recorder<T> internal constructor(val storageFrequency: StorageFrequency = StorageFrequency.All,
                                            val memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
                                            val diskDuration: StorageDuration = StorageDuration.Forever,
                                            val updatable: Updatable<ValueInstant<T>>,
                                            val dataDeserializer: (String) -> T) {

    val uid = getMemoryRecorderUid()

    private val currentFilesMap = HashMap<Instant, File>()

    private var jsonFactory = JsonFactory()

    //private var fileWriter: JsonGenerator = getNewFileWriter()

    private val fileChannel: AsynchronousFileChannel

    private var filePosition = 0L

    //private val jobQueue = LinkedBlockingQueue<Pair<RecorderAction,() -> Unit>>()

    //private val writeLock = ReentrantReadWriteLock()

    //private val jobLock = ReentrantReadWriteLock()

    private val executor = Executors.newSingleThreadExecutor()

    private var currentInterval: Long = 0

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
        currentFilesMap.put(now, file)
        return writer
    }

    private val _dataInMemory = ArrayList<ValueInstant<T>>()

    //TODO: Change to implementation of a truly immutable list.
    val dataInMemory: List<ValueInstant<T>> get() = ArrayList(_dataInMemory)

    val allData: Deferred<List<ValueInstant<T>>> get() = TODO("Implement")

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

        if (memoryDuration != StorageDuration.None) {
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
        } else {
            listenJob = updatable.openNewCoroutineListener {
                writeOut(it)
            }
        }
    }

    private fun cachePerNumMeasurement(value: ValueInstant<T>, samples: Int) {
        if (_dataInMemory.size >= samples) {
            writeOut(_dataInMemory)
            _dataInMemory.clear()
        } else {
            _dataInMemory.add(value)
        }
    }

    private fun checkFileAge() {
        if (diskDuration is StorageDuration.For) {
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

    private fun checkMemoryAge() {
        val now = Instant.now()
        if (memoryDuration is StorageDuration.For) {
            _dataInMemory.iterator().forEach {
                launch(daqcThreadContext) {
                    writeOut(it)
                    _dataInMemory.remove(it)
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

    private fun checkInterval(duration: Duration): Boolean {
        val potential = (Instant.now().toEpochMilli() / duration.toMillis())
        if (currentInterval - potential < 0) {
            currentInterval = potential
            return true
        }
        return false
    }

    private suspend fun cachePerInterval(value: ValueInstant<T>, duration: Duration) {
        if (checkInterval(duration)) {
            writeOut(_dataInMemory)
            _dataInMemory.clear()
        } else {
            _dataInMemory.add(value)
        }
    }

    private fun cacheAll(value: ValueInstant<T>) {
        _dataInMemory.add(value)
        if (memoryDuration == StorageDuration.Forever && diskDuration != StorageDuration.None) {
            writeOut(value)
        }
    }

    private fun writeOut(entry: List<ValueInstant<T>>) {
        if (diskDuration != StorageDuration.None) {
            entry.forEach { writeOut(it) }
        }
    }

    private fun writeOut(entry: ValueInstant<T>) {
        if (diskDuration != StorageDuration.None) {
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

    fun stop() {
        diskExpirationTask.cancel()
        listenJob.cancel()
        writeOut(_dataInMemory)
        launch(daqcThreadContext) {
            //TODO writeJsonBuffer("]")
            fileChannel.force(true)
            fileChannel.close()
        }
    }


    fun getDataInRange(instantRange: ClosedRange<Instant>): Deferred<List<ValueInstant<T>>> {
        val start = instantRange.start
        val end = instantRange.endInclusive

        val future: Future<List<ValueInstant<T>>> = executor.submit(Callable<List<ValueInstant<T>>> {
            val found = ArrayList<ValueInstant<T>>(
                    _dataInMemory.filter { it.instant.isAfter(start) && it.instant.isBefore(end) })
            if (!(_dataInMemory.filter { it.instant.isAfter(end) }.isNotEmpty())) {
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

        TODO("Implement")
    }

    companion object {
        private const val VALUE = "value"
        private const val TIME = "time"
    }
}