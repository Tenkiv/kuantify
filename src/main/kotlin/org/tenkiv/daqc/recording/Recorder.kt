package org.tenkiv.daqc.recording

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.isOlderThan
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.PrimitiveValueInstant
import org.tenkiv.daqc.Updatable
import org.tenkiv.daqc.daqcThreadContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.Charset
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.experimental.coroutineContext

typealias ValueSerializer<T> = (T) -> String
typealias ValueDeserializer<T> = (String) -> T

//TODO: Move default parameter values in recorder creation function to constants
fun <T, U : Updatable<ValueInstant<T>>> U.pairWithNewRecorder(
    storageFrequency: StorageFrequency = StorageFrequency.All,
    memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
    diskDuration: StorageDuration = StorageDuration.Forever,
    filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true },
    valueSerializer: (T) -> String,
    valueDeserializer: (String) -> T
) =
    RecordedUpdatable(
        this,
        Recorder(
            this,
            storageFrequency,
            memoryDuration,
            diskDuration,
            filterOnRecord,
            valueSerializer,
            valueDeserializer
        )
    )

fun <T, U : Updatable<ValueInstant<T>>> U.pairWithNewRecorder(
    storageFrequency: StorageFrequency = StorageFrequency.All,
    memoryDuration: StorageSamples = StorageSamples.Number(100),
    diskDuration: StorageSamples = StorageSamples.None,
    filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true },
    valueSerializer: (T) -> String,
    valueDeserializer: (String) -> T
) =
    RecordedUpdatable(
        this,
        Recorder(
            this,
            storageFrequency,
            memoryDuration,
            diskDuration,
            filterOnRecord,
            valueSerializer,
            valueDeserializer
        )
    )

fun <T, U : Updatable<ValueInstant<T>>> U.pairWithNewRecorder(
    storageFrequency: StorageFrequency = StorageFrequency.All,
    memoryStorageLength: StorageLength = StorageSamples.Number(100),
    filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true }
) =
    RecordedUpdatable(
        this,
        Recorder(
            this,
            storageFrequency,
            memoryStorageLength,
            filterOnRecord
        )
    )

fun <T> List<ValueInstant<T>>.getDataInRange(instantRange: ClosedRange<Instant>):
        List<ValueInstant<T>> {
    val oldestRequested = instantRange.start
    val newestRequested = instantRange.endInclusive

    return this.filter {
        it.instant.isAfter(oldestRequested) &&
                it.instant.isBefore(newestRequested) ||
                it.instant == oldestRequested ||
                it.instant == newestRequested
    }
}


data class RecordedUpdatable<out T, out U : Updatable<ValueInstant<T>>>(
    val updatable: U,
    val recorder: Recorder<T>
)

sealed class StorageFrequency {

    object All : StorageFrequency()

    data class Interval(val interval: Duration) : StorageFrequency()

    data class PerNumMeasurements(val number: Int) : StorageFrequency()
}

sealed class StorageLength

sealed class StorageSamples : StorageLength(), Comparable<StorageSamples> {

    object All : StorageSamples() {

        override fun compareTo(other: StorageSamples): Int =
            when (other) {
                is All -> 0
                else -> 1
            }

    }

    object None : StorageSamples() {

        override fun compareTo(other: StorageSamples): Int =
            when (other) {
                is None -> 0
                else -> -1
            }
    }

    data class Number(val numSamples: Int) : StorageSamples() {

        override fun compareTo(other: StorageSamples): Int =
            when (other) {
                is All -> -1
                is None -> 1
                is Number -> numSamples.compareTo(other.numSamples)
            }
    }

}

sealed class StorageDuration : StorageLength(), Comparable<StorageDuration> {

    object Forever : StorageDuration() {

        override fun compareTo(other: StorageDuration): Int =
            when (other) {
                is Forever -> 0
                else -> 1
            }

    }

    object None : StorageDuration() {

        override fun compareTo(other: StorageDuration): Int =
            when (other) {
                is None -> 0
                else -> -1
            }
    }

    data class For(val duration: Duration) : StorageDuration() {

        override fun compareTo(other: StorageDuration): Int =
            when (other) {
                is Forever -> -1
                is None -> 1
                is For -> duration.compareTo(other.duration)
            }
    }
}

class Recorder<out T> {

    val updatable: Updatable<ValueInstant<T>>
    val storageFrequency: StorageFrequency
    val memoryStorageLength: StorageLength
    val diskStorageLength: StorageLength
    private val filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean
    private val valueSerializer: ValueSerializer<T>?
    private val valueDeserializer: ValueDeserializer<T>?

    private val recordJob: Job

    private val receiveChannel: ReceiveChannel<ValueInstant<T>>

    private val uid = getRecorderUid()
    private val directoryPath = async(daqcThreadContext) { "$RECORDERS_PATH/${uid.await()}" }
    private val directoryFile = async(daqcThreadContext) { File(directoryPath.await()).apply { mkdir() } }

    private val _dataInMemory = ArrayList<ValueInstant<T>>()

    private val files = ArrayList<RecorderFile>()

    private val fileCreationBroadcaster = ConflatedBroadcastChannel<RecorderFile>()

    /**
     * @param filterOnRecord filters the incoming [ValueInstant]s should return true if the recorder should store the
     * ValueInstant and false if it should not.
     * @param valueSerializer returned String will be stored in a JSON file and should be a compliant JSON String.
     */
    constructor(
        updatable: Updatable<ValueInstant<T>>,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(memoryDurationDefault),
        diskDuration: StorageDuration = StorageDuration.None,
        filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true },
        valueSerializer: ValueSerializer<T>,
        valueDeserializer: ValueDeserializer<T>
    ) {
        this.updatable = updatable
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = memoryDuration
        this.diskStorageLength = diskDuration
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = valueSerializer
        this.valueDeserializer = valueDeserializer

        this.receiveChannel = updatable.broadcastChannel.openSubscription()
        this.recordJob = createRecordJob()
    }

    constructor(
        updatable: Updatable<ValueInstant<T>>,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        numSamplesMemory: StorageSamples = StorageSamples.Number(100),
        numSamplesDisk: StorageSamples = StorageSamples.None,
        filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true },
        valueSerializer: ValueSerializer<T>,
        valueDeserializer: ValueDeserializer<T>
    ) {
        this.updatable = updatable
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = numSamplesMemory
        this.diskStorageLength = numSamplesDisk
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = valueSerializer
        this.valueDeserializer = valueDeserializer

        this.receiveChannel = updatable.broadcastChannel.openSubscription()
        this.recordJob = createRecordJob()
    }

    constructor(
        updatable: Updatable<ValueInstant<T>>,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryStorageLength: StorageLength = StorageSamples.Number(100),
        filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true }
    ) {
        this.updatable = updatable
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = memoryStorageLength
        this.diskStorageLength = StorageSamples.None
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = null
        this.valueDeserializer = null

        this.receiveChannel = updatable.broadcastChannel.openSubscription()
        this.recordJob = createRecordJob()
    }


    /**
     * Gets the data currently stored in RAM by this recorder. The returned list can not be modified.
     * To get an updated list of data in memory, call this function again.
     */
    //TODO: Make this return truly immutable list.
    fun getDataInMemory(): List<ValueInstant<T>> = ArrayList(_dataInMemory)

    //TODO: Make this return a truly immutable list using a builder
    fun getAllData(): Deferred<List<ValueInstant<T>>> = async(daqcThreadContext) {
        if (memoryStorageLength >= diskStorageLength) getDataInMemory() else getDataFromDisk { true }
    }

    //TODO: Make similar extension function for Collection<ValueInstant<T>>
    fun getDataInRange(instantRange: ClosedRange<Instant>): Deferred<List<ValueInstant<T>>> =
        async(daqcThreadContext) {
            val oldestRequested = instantRange.start
            val newestRequested = instantRange.endInclusive

            val filterFun: (ValueInstant<T>) -> Boolean = {
                it.instant.isAfter(oldestRequested) &&
                        it.instant.isBefore(newestRequested) ||
                        it.instant == oldestRequested ||
                        it.instant == newestRequested
            }

            val oldestMemory = _dataInMemory.firstOrNull()
            if (oldestMemory != null && oldestMemory.instant.isBefore(oldestRequested))
                return@async _dataInMemory.filter(filterFun)
            else if (diskStorageLength > memoryStorageLength)
                getDataFromDisk(filterFun)
            else
                return@async _dataInMemory.filter(filterFun)
        }

    fun stop(shouldDeleteDiskData: Boolean = false) {
        receiveChannel.close()
        recordJob.cancel(CancellationException("Recorder manually stopped"))
        files.forEach { it.stop() }
        if (shouldDeleteDiskData)
            launch(daqcThreadContext) {
                directoryFile.await().delete()
            }
    }

    private suspend fun getDataFromDisk(filter: (ValueInstant<T>) -> Boolean): List<ValueInstant<T>> {
        //TODO: Change to immutable list using builder
        val result = ArrayList<ValueInstant<T>>()
        val currentFiles: List<RecorderFile> = ArrayList(files)

        val bufferMutex = Mutex()
        val buffer = ArrayList<ValueInstant<T>>()

        launch(daqcThreadContext) {
            fileCreationBroadcaster.openSubscription().receive()
            updatable.broadcastChannel.consumeEach { value ->
                bufferMutex.withLock {
                    buffer += value
                }
            }
        }

        currentFiles.forEach {
            result.addAll(it.readFromDisk(filter))
        }
        bufferMutex.withLock {
            result.addAll(buffer.filter(filter))
        }

        return result
    }

    private suspend fun recordUpdate(update: ValueInstant<T>) {
        if (filterOnRecord(update)) {
            if (memoryStorageLength !== StorageDuration.None) _dataInMemory += update

            files.lastOrNull()?.writeEntry(update)

            if (diskStorageLength is StorageSamples.Number) files.forEach { it.samplesSinceCreation++ }
        }
    }

    private fun cleanMemory() {
        if (memoryStorageLength is StorageDuration.For) {
            val iterator = _dataInMemory.iterator()
            while (iterator.hasNext())
                if (iterator.next().instant.isOlderThan(memoryStorageLength.duration))
                    iterator.remove()
                else
                    break
        }
        if (memoryStorageLength is StorageSamples.Number) {
            if (_dataInMemory.size > memoryStorageLength.numSamples) _dataInMemory.remove(_dataInMemory.first())
        }
    }

    // This has to be an extension function in order to keep out variance in Recorder class generic
    private suspend fun RecorderFile.writeEntry(entry: ValueInstant<T>) {
        if (valueSerializer != null) {
            val jsonObjectString =
                "{\"$INSTANT_KEY\":${entry.instant.toEpochMilli()}," +
                        "\"$VALUE_KEY\":${valueSerializer.invoke(entry.value)}}"
            writeJsonBuffer(jsonObjectString)
        } else {
            throw IllegalStateException("valueSerializer cannot be null if Recorder is using disk for storage.")
        }
    }

    private fun createRecordJob() = launch(daqcThreadContext) {
        if (diskStorageLength === StorageDuration.Forever)
            files += RecorderFile(expiresIn = null)

        if (diskStorageLength is StorageDuration.For)
            launch(coroutineContext) {
                val fileCreationInterval = diskStorageLength.duration.dividedBy(10)
                val fileExpiresIn = diskStorageLength.duration + diskStorageLength.duration.dividedBy(9)
                while (isActive) {
                    val newRecorderFile = RecorderFile(fileExpiresIn)
                    files += newRecorderFile
                    fileCreationBroadcaster.send(newRecorderFile)
                    delay(fileCreationInterval.toMillis())
                }
            }

        if (diskStorageLength is StorageSamples.Number) {
            launch(coroutineContext) {
                val fileCreationInterval = diskStorageLength.numSamples / 10
                val fileExpiresIn = (diskStorageLength.numSamples + diskStorageLength.numSamples / 9) + 1

                files += RecorderFile(fileExpiresIn)
                val receiveChannel = updatable.broadcastChannel.openSubscription()
                while (isActive) {
                    val newRecorderFile = RecorderFile(fileExpiresIn)
                    if (files.last().samplesSinceCreation == fileCreationInterval) files += newRecorderFile
                    fileCreationBroadcaster.send(newRecorderFile)
                    receiveChannel.receive()
                }
            }
        }

        if (storageFrequency is StorageFrequency.Interval)
            while (isActive) {
                delay(storageFrequency.interval.toMillis())
                recordUpdate(updatable.getValue())
                cleanMemory()
            }
        else {
            var numUnstoredMeasurements = 0

            receiveChannel.consumeEach { update ->
                if (storageFrequency === StorageFrequency.All) {
                    recordUpdate(update)
                    cleanMemory()
                }
                if (storageFrequency is StorageFrequency.PerNumMeasurements) {
                    numUnstoredMeasurements++
                    if (numUnstoredMeasurements == storageFrequency.number) {
                        recordUpdate(update)
                        cleanMemory()
                        numUnstoredMeasurements = 0
                    }
                }
            }
        }
    }

    //TODO: Throw more specific exception if the class cannot be cast.
    private operator fun StorageLength.compareTo(other: StorageLength): Int = when (this) {
        is StorageDuration -> this.compareTo(other as StorageDuration)
        is StorageSamples -> this.compareTo(other as StorageSamples)
    }

    //▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
    //   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Inner class ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
    //▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
    private inner class RecorderFile {

        internal val file = async(daqcThreadContext) {
            File("${directoryPath.await()}/${System.currentTimeMillis()}.json")
        }
        internal val fileChannel = async(daqcThreadContext) {
            AsynchronousFileChannel.open(
                file.await().toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
        }

        private val mutex = Mutex()

        private var arrayLastPosition = 0L

        private var isFirstWrite = true

        // Only incremented if the disk storage is set to a specific number of samples.
        internal var samplesSinceCreation = 0

        internal constructor(expiresIn: Duration?) {
            if (expiresIn != null) {
                launch(daqcThreadContext) {
                    delay(expiresIn.toMillis())

                    suicide()
                }
            }
        }

        internal constructor(expiresAfterNumSamples: Int?) {
            if (expiresAfterNumSamples != null) {
                launch(daqcThreadContext) {
                    val receiveChannel = updatable.broadcastChannel.openSubscription()

                    while (samplesSinceCreation < expiresAfterNumSamples) {
                        receiveChannel.receive()
                    }

                    suicide()
                }
            }
        }

        internal suspend fun writeJsonBuffer(jsonObjectString: String) {
            val string: String
            if (isFirstWrite) {
                string = "$ARRAY_OPEN$jsonObjectString$ARRAY_CLOSE"
                writeToFile(string)
                isFirstWrite = false
            } else {
                string = ",$jsonObjectString$ARRAY_CLOSE"
                writeToFile(string)
            }
            arrayLastPosition += string.length - ARRAY_CLOSE.length
        }

        private suspend fun writeToFile(
            string: String,
            charset: Charset = Charset.defaultCharset()
        ) {
            val buffer = ByteBuffer.wrap(string.toByteArray(charset))
            mutex.withLock {
                fileChannel.await().aWrite(buffer, arrayLastPosition)
            }
        }

        internal fun stop() {
            launch(daqcThreadContext) {
                val channel = fileChannel.await()
                mutex.withLock {
                    //TODO: Change this to blocking() call in kotlin coroutines.
                    channel.force(true)
                    channel.close()
                }

            }
        }

        private suspend fun suicide() {
            files.remove(this@RecorderFile)
            fileChannel.await().close()
            file.await().delete()
        }

        internal suspend fun readFromDisk(filter: (ValueInstant<T>) -> Boolean): List<ValueInstant<T>> {
            if (valueDeserializer != null) {
                val channel = fileChannel.await()
                var inString = false
                var numUnclosedBraces = 0
                var previousCharByte = ' '.toByte()
                val complyingObjects = ArrayList<ValueInstant<T>>()
                val currentObject = ArrayList<Byte>()
                var buffer = ByteBuffer.allocate(100)
                var position = 0L

                while (channel.isOpen) {
                    mutex.withLock { channel.aRead(buffer, position) }

                    val array = buffer.array()
                    array.forEach { charByte ->
                        if (charByte == STRING_DELIM && previousCharByte != BREAK)
                            inString = !inString

                        if (!inString && charByte == OPEN_BRACE)
                            numUnclosedBraces++

                        if (numUnclosedBraces > 1)
                            currentObject += charByte


                        if (!inString && charByte == CLOSE_BRACE) {
                            numUnclosedBraces--
                            if (numUnclosedBraces == 0)
                                return@readFromDisk complyingObjects

                            if (numUnclosedBraces == 1) {

                                println("Byte Array = ${String(currentObject.toByteArray())}")

                                val jsonTree = jacksonMapper.readTree(currentObject.toByteArray())
                                val epochMilli = jsonTree[INSTANT_KEY].asLong()
                                val valueTree = jsonTree[VALUE_KEY]
                                val valueString = if (valueTree.isTextual) valueTree.asText() else valueTree.toString()
                                println("epochMilli=$epochMilli, valueString=$valueString")
                                val valueInstant =
                                    PrimitiveValueInstant(epochMilli, valueString)
                                        .toValueInstant(valueDeserializer)


                                if (filter(valueInstant)) {
                                    complyingObjects += valueInstant
                                }
                                currentObject.clear()

                            }

                        }

                        previousCharByte = charByte
                    }
                    position += 100
                    buffer = ByteBuffer.allocate(100)

                }
                return complyingObjects
            } else {
                throw IllegalStateException("valueDeserializer cannot be null if recorder is using disk for storage.")
            }
        }

    }

    companion object {
        @PublishedApi
        internal val memoryDurationDefault = 30L.secondsSpan

        // The value of this must match the corresponding property name in PrimitiveValueInstant.
        private const val VALUE_KEY = "value"
        // The value of this must match the corresponding property name in PrimitiveValueInstant.
        private const val INSTANT_KEY = "epochMilli"
        private const val OPEN_BRACE = '{'.toByte()
        private const val CLOSE_BRACE = '}'.toByte()
        private const val STRING_DELIM = '"'.toByte()
        private const val BREAK = '\\'.toByte()
        private const val ZERO_BYTE: Byte = 0
        private const val ARRAY_OPEN = "{\"entries\":["
        private const val ARRAY_CLOSE = "]}"

        private const val RECORDERS_PATH = "recorders"

        private val jacksonMapper: ObjectMapper = jacksonObjectMapper()

        private val recordersDirectory = File(RECORDERS_PATH).apply { mkdir() }

        //TODO: Change to ReadWriteMutex
        private val recorderUidMutex = Mutex()
        private var recorderUid: Long? = null

        private fun getRecorderUid(): Deferred<String> = async(daqcThreadContext) {
            recorderUidMutex.withLock {
                val lastUid = recorderUid
                val thisUid: Long

                if (lastUid != null) {
                    thisUid = lastUid + 1
                    recorderUid = thisUid
                } else {
                    thisUid = recordersDirectory.listFiles()
                        .map { it.name.toLongOrNull() }
                        .requireNoNulls()
                        .max()?.plus(1) ?: 1L
                    recorderUid = thisUid
                }
                thisUid.toString()
            }

        }

        /**
         * This is a blocking call.
         */
        fun deleteAllRecordsFromDisk() = recordersDirectory.delete()

    }
}