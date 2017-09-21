package org.tenkiv.daqc.recording

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.StoredData
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.withALock
import org.tenkiv.daqcThreadContext
import org.tenkiv.getMemoryRecorderUid
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.Charset
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList


open class Recorder<out T> internal constructor(val storageFrequency: StorageFrequency = StorageFrequency.All,
                                                val memoryDuration: StorageDuration =
                                                StorageDuration.For(30L.secondsSpan),
                                                val diskDuration: StorageDuration = StorageDuration.Forever,
                                                updatable: Updatable<ValueInstant<T>>,
                                                val dataDeserializer: (String) -> T) {

    val uid = getMemoryRecorderUid()

    private val currentFilesMap = HashMap<Instant, File>()
    private val currentFileChannelsMap = HashMap<Instant, AsynchronousFileChannel>()

    private var perCountFrequencyCounter = 0

    private var fileChannel: AsynchronousFileChannel

    private var filePosition = 0L

    private var currentInterval: Long = 0

    private var isFirst = true

    private fun getNewFileWriter(): AsynchronousFileChannel {
        val now = Instant.now()
        val file = File("tempStorage_${uid}_${now.toEpochMilli()}.json")
        val writer = AsynchronousFileChannel.open(file.toPath(),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE)
        isFirst = true
        if (currentFilesMap.size != 0) {
            if (fileChannel.isOpen) {
                launch(daqcThreadContext) {
                    writeJsonBuffer("]", fileChannel)
                    // fileChannel.close()
                }
            }
        }

        launch(daqcThreadContext) {
            writeJsonBuffer("[", writer)
            writer.force(false)
        }

        currentFileChannelsMap.put(now, writer)
        currentFilesMap.put(now, file)
        return writer
    }

    private val _dataInMemory = ArrayList<ValueInstant<T>>()

    //TODO: Change to implementation of a truly immutable list.
    val dataInMemory: List<ValueInstant<T>> get() = ArrayList(_dataInMemory)

    val allData: Deferred<List<ValueInstant<T>>> get() = getDataInRange(Instant.MIN..Instant.MAX)

    private val subRecChannel: SubscriptionReceiveChannel<ValueInstant<T>>

    private var diskRefreshJob: Job? = null

    private var memoryRefreshJob: Job? = null

    init {
        fileChannel = getNewFileWriter()

        if (diskDuration is StorageDuration.For) {
            val delay = diskDuration.duration.toMillis() / 2
            diskRefreshJob = launch(daqcThreadContext) {
                while (isActive) {
                    delay(delay)
                    fileChannel = getNewFileWriter()
                    checkFileAge()
                }
            }
        }

        if (memoryDuration is StorageDuration.For) {
            val delay = memoryDuration.duration.toMillis() / 2
            memoryRefreshJob = launch(daqcThreadContext) {
                while (isActive) {
                    delay(delay)
                    checkMemoryAge()
                }
            }
        }

        subRecChannel = updatable.broadcastChannel.openSubscription()
        launch(daqcThreadContext) {
            if (memoryDuration != StorageDuration.None) {
                subRecChannel.consumeEach {
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
                subRecChannel.consumeEach {
                    writeOut(it)
                }
            }
        }
    }

    private suspend fun cachePerNumMeasurement(value: ValueInstant<T>, samples: Int) {
        if (perCountFrequencyCounter >= samples) {
            _dataInMemory.add(value)
        }
    }

    private fun checkFileAge() {
        if (diskDuration is StorageDuration.For) {
            val now = Instant.now()
            currentFilesMap.iterator().forEach {
                if (Duration.between(it.key, now) > diskDuration.duration) {
                    launch(daqcThreadContext) {
                        currentFileChannelsMap[it.key]?.close()
                        currentFilesMap[it.key]?.delete()
                        currentFilesMap.remove(it.key)
                    }
                }
            }
        }
    }

    private suspend fun checkMemoryAge() {
        val now = Instant.now()
        if (memoryDuration is StorageDuration.For) {
            _dataInMemory.iterator().forEach {
                if (Duration.between(it.instant, now) > memoryDuration.duration) {
                    launch(daqcThreadContext) {
                        writeOut(it)
                        _dataInMemory.remove(it)
                    }
                }
            }
        }
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

    private suspend fun cacheAll(value: ValueInstant<T>) {
        _dataInMemory.add(value)
        if (memoryDuration == StorageDuration.Forever && diskDuration != StorageDuration.None) {
            writeOut(value)
        }
    }

    private suspend fun writeOut(entry: List<ValueInstant<T>>) {
        if (diskDuration != StorageDuration.None) {
            entry.forEach { writeOut(it) }
        }

    }

    private suspend fun writeOut(entry: ValueInstant<T>) {
        if (diskDuration != StorageDuration.None) {
            try {
                writeJsonBuffer(
                        "${if (isFirst) {
                            isFirst = false;""
                        } else {
                            ","
                        }}{\"$TIME\":${entry.instant.toEpochMilli()}," +
                                "\"$VALUE\":\"${entry.value}\"}", fileChannel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun writeJsonBuffer(string: String,
                                        fileChannel: AsynchronousFileChannel,
                                        charset: Charset = Charset.defaultCharset()) {

        try {
            fileChannel.withALock {
                val buffer = ByteBuffer.wrap(string.toByteArray(charset))
                println("$filePosition Writing: ${String(buffer.array())}")
                    fileChannel.aWrite(buffer, filePosition)
                filePosition += string.length
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop(deleteDiskData: Boolean = true) {
        println("Stop 1")
        launch(daqcThreadContext) {

            diskRefreshJob?.cancel()
            memoryRefreshJob?.cancel()
            subRecChannel.close()
            println("Stop2 " + deleteDiskData)
            writeOut(_dataInMemory)
            println("Stop3")
            if (!deleteDiskData) {

                //mutex.withLock {
                fileChannel.withALock {
                    fileChannel.force(false)

                }
                writeJsonBuffer("]", fileChannel)
                //fileChannel.close()

                //}
            } else {
                //fileChannel.close()
                currentFilesMap.forEach {
                    currentFileChannelsMap[it.key]?.withALock {
                        currentFileChannelsMap[it.key]?.close()
                        if (it.value.exists()) {
                            it.value.delete()
                        }
                    }
                }
            }
        }
    }

    fun getAllDataFromDisk(): Deferred<List<ValueInstant<T>>> = async(daqcThreadContext) { getDataFromDisk() }

    //TODO: Make matching extension function for Collection<ValueInstant<T>>
    fun getDataInRange(instantRange: ClosedRange<Instant>): Deferred<List<ValueInstant<T>>> =
            async(daqcThreadContext) {
                val start = instantRange.start
                val end = instantRange.endInclusive

                val filterFun: (ValueInstant<T>) -> Boolean = { it.instant.isAfter(start) && it.instant.isBefore(end) }

                if (start.isBefore(Instant.now())) {
                    throw IllegalArgumentException("Requested start time is in the future.")
                }

                val found = ArrayList<ValueInstant<T>>(_dataInMemory.filter(filterFun))

                if (!(_dataInMemory.filter { it.instant.isAfter(end) }.isNotEmpty())) {
                    return@async found
                } else {
                    getDataFromDisk(filterFun, found)
                }

                if (found.sortedBy { it.instant }.last().instant.isBefore(end)) {
                    throw IllegalArgumentException("Last possible Instant is before last requested Instant")
                }
                return@async found
            }

    private suspend fun getDataFromDisk(filter: (ValueInstant<T>) -> Boolean = { true },
                                        aggregatedList: MutableList<ValueInstant<T>> = ArrayList()):
            List<ValueInstant<T>> {

        currentFileChannelsMap.toSortedMap().forEach {
            //mutex.withLock {
                aggregatedList.addAll(readFromDisk(it.value, filter))
            //}
        }
        return aggregatedList
    }

    fun getMatchingData(filter: (ValueInstant<T>) -> Boolean): Deferred<Collection<ValueInstant<T>>> =
            async(daqcThreadContext) {
                val found = ArrayList<ValueInstant<T>>(_dataInMemory.filter { filter(it) })
                getDataFromDisk(filter, found)
                return@async found
            }

    companion object {
        private const val VALUE = "value"
        private const val TIME = "time"
    }

    private val jacksonMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

    private val OBJECT_START = '{'.toByte()
    private val OBJECT_END = '}'.toByte()

    private val STRING_DELIM = '"'.toByte()

    private val ZERO_BYTE: Byte = 0
    private var zeroCount = 0

    private suspend fun readFromDisk(channel: AsynchronousFileChannel, filter: (ValueInstant<T>) -> Boolean = { true }): List<ValueInstant<T>> {
        /*val channel = AsynchronousFileChannel.open(file.toPath(),
                StandardOpenOption.READ)*/

        var objectStart = false
        var isInString = false
        val complyingObjects = ArrayList<ValueInstant<T>>()
        val currentObject = ArrayList<Byte>()
        var buff = ByteBuffer.allocate(100)
        var position = 0L

        while (channel.isOpen) {
            try {
                //channel.withALock {
                channel.aRead(buff, position)
                //}
                val array = buff.array()
                array.forEach {
                    if (objectStart && it == STRING_DELIM) {
                        isInString = !isInString
                    }

                    if (!isInString && it == OBJECT_START) objectStart = true

                    if (objectStart) {
                        currentObject.add(it)
                    }

                    if (!isInString && it == OBJECT_END) {
                        objectStart = false
                        val value = jacksonMapper.readValue<StoredData>(currentObject.toByteArray()).
                                getValueInstant(dataDeserializer)
                        if (filter(value)) {
                            complyingObjects.add(value)
                        }
                        currentObject.clear()
                    }

                    if (it == ZERO_BYTE) {
                        zeroCount++
                        if (zeroCount >= array.size) {
                            return@readFromDisk complyingObjects
                        }
                    }
                }
                position += 100
                buff = ByteBuffer.allocate(100)
            }catch (e:Exception){
               e.printStackTrace()
            }
        }
        return complyingObjects
    }
}