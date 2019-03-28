/*
 * Copyright 2019 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.kuantify.recording

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.*
import kotlinx.coroutines.time.*
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.lib.*
import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.charset.*
import java.nio.file.*
import java.time.*
import kotlin.coroutines.*

public typealias ValueSerializer<T> = (T) -> String
public typealias ValueDeserializer<T> = (String) -> T
public typealias RecordingFilter<T, U> = Recorder<T, U>.(ValueInstant<T>) -> Boolean
internal typealias StorageFilter<T> = (ValueInstant<T>) -> Boolean

//TODO: Move default parameter values in recorder creation function to constants
public fun <T, U : Trackable<ValueInstant<T>>> CoroutineScope.Recorder(
    updatable: U,
    storageFrequency: StorageFrequency = StorageFrequency.All,
    memoryDuration: StorageDuration = StorageDuration.For(Recorder.memoryDurationDefault),
    diskDuration: StorageDuration = StorageDuration.None,
    filterOnRecord: RecordingFilter<T, U> = { true },
    valueSerializer: ValueSerializer<T>,
    valueDeserializer: ValueDeserializer<T>
): Recorder<T, U> = Recorder(
    scope = this,
    updatable = updatable,
    storageFrequency = storageFrequency,
    memoryDuration = memoryDuration,
    diskDuration = diskDuration,
    filterOnRecord = filterOnRecord,
    valueSerializer = valueSerializer,
    valueDeserializer = valueDeserializer
)

public fun <T, U : Trackable<ValueInstant<T>>> CoroutineScope.Recorder(
    updatable: U,
    storageFrequency: StorageFrequency = StorageFrequency.All,
    numSamplesMemory: StorageSamples = StorageSamples.Number(100),
    numSamplesDisk: StorageSamples = StorageSamples.None,
    filterOnRecord: RecordingFilter<T, U> = { true },
    valueSerializer: ValueSerializer<T>,
    valueDeserializer: ValueDeserializer<T>
): Recorder<T, U> = Recorder(
    scope = this,
    updatable = updatable,
    storageFrequency = storageFrequency,
    numSamplesMemory = numSamplesMemory,
    numSamplesDisk = numSamplesDisk,
    filterOnRecord = filterOnRecord,
    valueSerializer = valueSerializer,
    valueDeserializer = valueDeserializer
)

public fun <T, U : Trackable<ValueInstant<T>>> CoroutineScope.Recorder(
    updatable: U,
    storageFrequency: StorageFrequency = StorageFrequency.All,
    memoryStorageLength: StorageLength = StorageSamples.Number(100),
    filterOnRecord: RecordingFilter<T, U> = { true }
): Recorder<T, U> = Recorder(
    scope = this,
    updatable = updatable,
    storageFrequency = storageFrequency,
    memoryStorageLength = memoryStorageLength,
    filterOnRecord = filterOnRecord
)

// TODO: Use this from coral
public fun <T> Iterable<ValueInstant<T>>.getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<T>> =
    this.filter { it.instant in instantRange }

/**
 * Sealed class denoting the frequency at which samples should be stored.
 */
public sealed class StorageFrequency {

    /**
     * Store all data received.
     */
    public object All : StorageFrequency()

    /**
     * Store the data within an interval.
     *
     * @param interval The [Duration] over which samples will be stored.
     */
    public data class Interval(val interval: Duration) : StorageFrequency()

    /**
     * Store data per number of measurements received.
     *
     * @param number The interval of samples at which to store a new sample.
     */
    public data class PerNumMeasurements(val number: Int) : StorageFrequency()
}

/**
 * Sealed class for how long data should be stored either in memory or on disk.
 */
public sealed class StorageLength

/**
 * Sealed class denoting the number of samples to be kept in either memory or on disk.
 */
public sealed class StorageSamples : StorageLength(), Comparable<StorageSamples> {

    /**
     * Keep all data unless otherwise noted.
     */
    public object All : StorageSamples() {

        public override fun compareTo(other: StorageSamples): Int =
            when (other) {
                is All -> 0
                else -> 1
            }

    }

    /**
     * Keep no data
     */
    public object None : StorageSamples() {

        public override fun compareTo(other: StorageSamples): Int =
            when (other) {
                is None -> 0
                else -> -1
            }
    }

    /**
     * Keep a specific number of samples in memory or on disk.
     *
     * @param numSamples The number of samples to keep in memory or on disk.
     */
    public data class Number(val numSamples: Int) : StorageSamples() {

        public override fun compareTo(other: StorageSamples): Int =
            when (other) {
                is All -> -1
                is None -> 1
                is Number -> numSamples.compareTo(other.numSamples)
            }
    }

}

/**
 * Sealed class denoting the length of time which a sample should be kept in memory or on disk.
 */
public sealed class StorageDuration : StorageLength(), Comparable<StorageDuration> {

    /**
     * Keep the data without respect to time.
     */
    public object Forever : StorageDuration() {

        public override fun compareTo(other: StorageDuration): Int =
            when (other) {
                is Forever -> 0
                else -> 1
            }

    }

    /**
     * Keep none of the data.
     */
    public object None : StorageDuration() {

        public override fun compareTo(other: StorageDuration): Int =
            when (other) {
                is None -> 0
                else -> -1
            }
    }

    /**
     * Keep the data for a specified duration.
     *
     * @param duration The [Duration] with which to keep the data.
     */
    public data class For(val duration: Duration) : StorageDuration() {

        public override fun compareTo(other: StorageDuration): Int =
            when (other) {
                is Forever -> -1
                is None -> 1
                is For -> duration.compareTo(other.duration)
            }
    }
}

/**
 * Recorder to store data either in memory, on disk, or both depending on certain parameters.
 */
public class Recorder<out T, out U : Trackable<ValueInstant<T>>> : CoroutineScope {

    public val updatable: U
    public val storageFrequency: StorageFrequency
    public val memoryStorageLength: StorageLength
    public val diskStorageLength: StorageLength
    private val filterOnRecord: RecordingFilter<T, U>
    private val valueSerializer: ValueSerializer<T>?
    private val valueDeserializer: ValueDeserializer<T>?

    private val job: Job
    public override val coroutineContext: CoroutineContext

    private val receiveChannel: ReceiveChannel<ValueInstant<T>>

    private val uid = GlobalScope.async(Dispatchers.Daqc) { getRecorderUid() }
    private val directoryPath = GlobalScope.async(Dispatchers.Daqc) { "$RECORDERS_PATH/${uid.await()}" }
    private val directoryFile = GlobalScope.async(Dispatchers.Daqc) { File(directoryPath.await()).apply { mkdir() } }

    private val _dataInMemory = ArrayList<ValueInstant<T>>()

    private val files = ArrayList<RecorderFile>()

    private val fileCreationBroadcaster = ConflatedBroadcastChannel<RecorderFile>()

    /**
     * Recorder to store data either in memory or on disk depending on certain parameters.
     *
     * @param updatable The [Trackable] to monitor for samples.
     * @param storageFrequency Determines how frequently data is stored to the [Recorder].
     * @param memoryDuration Determines how long samples are stored in memory.
     * @param diskDuration Determines how long samples are stored on disk.
     * @param filterOnRecord filters the incoming [ValueInstant]s should return true if the recorder should store the
     * ValueInstant and false if it should not.
     * @param valueSerializer Returned String will be stored in a JSON file and should be a compliant JSON String.
     * @param valueDeserializer Function to deserialize the data from JSON to the original object.
     */
    @PublishedApi
    internal constructor(
        scope: CoroutineScope,
        updatable: U,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(memoryDurationDefault),
        diskDuration: StorageDuration = StorageDuration.None,
        filterOnRecord: RecordingFilter<T, U> = { true },
        valueSerializer: ValueSerializer<T>,
        valueDeserializer: ValueDeserializer<T>
    ) {
        this.job = Job(scope.coroutineContext[Job])
        this.coroutineContext = scope.coroutineContext + job

        this.updatable = updatable
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = memoryDuration
        this.diskStorageLength = diskDuration
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = valueSerializer
        this.valueDeserializer = valueDeserializer

        this.receiveChannel = updatable.updateBroadcaster.openSubscription()
    }

    /**
     * Recorder to store data either in memory or on disk depending on certain parameters.
     *
     * @param updatable The [Trackable] to monitor for samples.
     * @param storageFrequency Determines how frequently data is stored to the [Recorder].
     * @param numSamplesMemory Determines how long samples are stored in memory.
     * @param numSamplesDisk Determines how long samples are stored on disk.
     * @param filterOnRecord filters the incoming [ValueInstant]s should return true if the recorder should store the
     * ValueInstant and false if it should not.
     * @param valueSerializer Returned String will be stored in a JSON file and should be a compliant JSON String.
     * @param valueDeserializer Function to deserialize the data from JSON to the original object.
     */
    @PublishedApi
    internal constructor(
        scope: CoroutineScope,
        updatable: U,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        numSamplesMemory: StorageSamples = StorageSamples.Number(100),
        numSamplesDisk: StorageSamples = StorageSamples.None,
        filterOnRecord: RecordingFilter<T, U> = { true },
        valueSerializer: ValueSerializer<T>,
        valueDeserializer: ValueDeserializer<T>
    ) {
        this.job = Job(scope.coroutineContext[Job])
        this.coroutineContext = scope.coroutineContext + job

        this.updatable = updatable
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = numSamplesMemory
        this.diskStorageLength = numSamplesDisk
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = valueSerializer
        this.valueDeserializer = valueDeserializer

        this.receiveChannel = updatable.updateBroadcaster.openSubscription()
    }

    /**
     * Recorder to store data either in memory or on disk depending on certain parameters.
     *
     * @param updatable The [Trackable] to monitor for samples.
     * @param storageFrequency Determines how frequently data is stored to the [Recorder].
     * @param memoryStorageLength Determines how long samples are stored in memory.
     * @param filterOnRecord filters the incoming [ValueInstant]s should return true if the recorder should store the
     * ValueInstant and false if it should not.
     */
    @PublishedApi
    internal constructor(
        scope: CoroutineScope,
        updatable: U,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryStorageLength: StorageLength = StorageSamples.Number(100),
        filterOnRecord: RecordingFilter<T, U> = { true }
    ) {
        this.job = Job(scope.coroutineContext[Job])
        this.coroutineContext = scope.coroutineContext + job

        this.updatable = updatable
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = memoryStorageLength
        this.diskStorageLength = StorageSamples.None
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = null
        this.valueDeserializer = null

        this.receiveChannel = updatable.updateBroadcaster.openSubscription()
    }

    init {
        createRecordJob()
    }

    /**
     * Gets the data currently stored in heap memory by this recorder. The returned list can not be modified.
     * To get an updated list of data in memory, call this function again.
     *
     * @return A [List] of [ValueInstant]s of the data in heap memory sent by the Recorder's [Trackable].
     */
    //TODO: Make this return truly immutable list.
    public fun getDataInMemory(): List<ValueInstant<T>> = ArrayList(_dataInMemory)

    /**
     * Gets all data both in heap memory and disk. If disk data exists the function will suspend until data is restored
     * to heap memory.
     *
     * @return A [List] of [ValueInstant]s of all the data sent by the Recorder's [Trackable].
     */
    public suspend fun getAllData(): List<ValueInstant<T>> =
        if (memoryStorageLength >= diskStorageLength) {
            getDataInMemory()
        } else {
            withContext(coroutineContext + Dispatchers.Daqc) { getDataFromDisk { true } }
        }

    /**
     * Gets all the data between two points in time denoted by [Instant]s. If disk data is in range the function will
     * suspend until data is restored to heap memory.
     *
     * @param instantRange The range of time over which to get data.
     * @return A [List] of [ValueInstant]s stored by the recorder within the [ClosedRange].
     */
    public suspend fun getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<T>> =
        withContext(coroutineContext + Dispatchers.Daqc) {
            val oldestRequested = instantRange.start

            val filterFun: StorageFilter<T> = { it.instant in instantRange }

            val oldestMemory = _dataInMemory.firstOrNull()
            return@withContext if (oldestMemory != null && oldestMemory.instant.isBefore(oldestRequested)) {
                _dataInMemory.filter(filterFun)
            } else if (diskStorageLength > memoryStorageLength) {
                getDataFromDisk(filterFun)
            } else {
                _dataInMemory.filter(filterFun)
            }
        }

    /**
     * Stops the recorder.
     *
     * @param shouldDeleteDiskData If the recorder should delete the data stored on disk.
     */
    public fun cancel(shouldDeleteDiskData: Boolean = false): Job = launch {
        receiveChannel.cancel()

        val stoppingFiles = ArrayList<Job>()
        files.forEach { file ->
            stoppingFiles += launch { file.cancel() }
        }
        if (shouldDeleteDiskData) {
            withContext(Dispatchers.IO) {
                directoryFile.await().delete()
            }
        }

        stoppingFiles.joinAll()
        job.cancel()
    }

    // Can only be called from Contexts.Daqc
    private suspend fun getDataFromDisk(filter: StorageFilter<T>): List<ValueInstant<T>> {
        //TODO: Change to immutable list using builder
        val result = ArrayList<ValueInstant<T>>()
        val currentFiles: List<RecorderFile> = ArrayList(files)

        val buffer = ArrayList<ValueInstant<T>>()

        val bufferJob = launch(Dispatchers.Daqc) {
            fileCreationBroadcaster.openSubscription().receive()
            updatable.updateBroadcaster.consumeEach { value ->
                buffer += value
            }
        }

        currentFiles.forEach {
            if (it.isOpen.await()) result.addAll(it.readFromDisk(filter))
        }

        result.addAll(buffer.filter(filter))


        bufferJob.cancel()
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
            while (iterator.hasNext()) {
                if (iterator.next().instant.isOlderThan(memoryStorageLength.duration)) {
                    iterator.remove()
                } else {
                    break
                }
            }
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

    private fun createRecordJob() = launch(Dispatchers.Daqc) {
        if (diskStorageLength === StorageDuration.Forever) {
            files += RecorderFile(expiresIn = null)
        }

        if (diskStorageLength is StorageDuration.For) {
            launch {
                val fileCreationInterval = diskStorageLength.duration.dividedBy(10)
                val fileExpiresIn = diskStorageLength.duration + diskStorageLength.duration.dividedBy(9)
                while (isActive) {
                    val newRecorderFile = RecorderFile(fileExpiresIn)
                    files += newRecorderFile
                    fileCreationBroadcaster.send(newRecorderFile)
                    delay(fileCreationInterval)
                }
            }
        }

        if (diskStorageLength is StorageSamples.Number) {
            launch {
                val fileCreationInterval = diskStorageLength.numSamples / 10
                val fileExpiresIn = (diskStorageLength.numSamples + diskStorageLength.numSamples / 9) + 1

                files += RecorderFile(fileExpiresIn)
                val receiveChannel = updatable.updateBroadcaster.openSubscription()
                while (isActive) {
                    val newRecorderFile = RecorderFile(fileExpiresIn)
                    if (files.last().samplesSinceCreation == fileCreationInterval) files += newRecorderFile
                    fileCreationBroadcaster.send(newRecorderFile)
                    receiveChannel.receive()
                }
            }
        }

        if (storageFrequency is StorageFrequency.Interval) {
            while (isActive) {
                delay(storageFrequency.interval)
                recordUpdate(updatable.getValue())
                cleanMemory()
            }
        } else {
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

        internal val file = async {
            File("${directoryPath.await()}/${System.currentTimeMillis()}.json")
        }
        internal val fileChannel = async {
            AsynchronousFileChannel.open(
                file.await().toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
        }

        internal val isOpen get() = async { fileChannel.await().isOpen }

        private val mutex = Mutex()

        private var arrayLastPosition = 0L

        private var isFirstWrite = true

        // Only incremented if the disk storage is set to a specific number of samples.
        internal var samplesSinceCreation = 0

        internal constructor(expiresIn: Duration?) {
            if (expiresIn != null) {
                launch(Dispatchers.Daqc) {
                    delay(expiresIn)

                    suicide()
                }
            }
        }

        internal constructor(expiresAfterNumSamples: Int?) {
            if (expiresAfterNumSamples != null) {
                launch(Dispatchers.Daqc) {
                    val receiveChannel = updatable.updateBroadcaster.openSubscription()

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

        internal suspend fun cancel() {
            withContext(Dispatchers.IO) {
                val channel = fileChannel.await()
                mutex.withLock {
                    channel.force(true)
                    channel.close()
                }
            }
        }

        private suspend fun suicide() {
            withContext(Dispatchers.Daqc) {
                files.remove(this@RecorderFile)
            }
            withContext(Dispatchers.IO + NonCancellable) {
                mutex.withLock {
                    fileChannel.await().close()
                    file.await().delete()
                }
            }
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
                        if (charByte == STRING_DELIM && previousCharByte != BREAK) inString = !inString

                        if (!inString && charByte == OPEN_BRACE) numUnclosedBraces++

                        if (numUnclosedBraces > 1) currentObject += charByte


                        if (!inString && charByte == CLOSE_BRACE) {
                            numUnclosedBraces--
                            if (numUnclosedBraces == 0) return@readFromDisk complyingObjects

                            if (numUnclosedBraces == 1) {
                                val valueInstant = deserializeValueInstant(currentObject, valueDeserializer)

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

        private fun deserializeValueInstant(
            jsonObject: List<Byte>,
            valueDeserializer: ValueDeserializer<T>
        ): ValueInstant<T> {
            val jsonTree = Json.plain.parseJson(jsonObject.toString()).jsonObject
            val epochMilli = jsonTree[INSTANT_KEY].long
            val valueTree = jsonTree[VALUE_KEY]
            val valueString = valueTree.contentOrNull ?: valueTree.toString()

            return PrimitiveValueInstant(epochMilli, valueString).toValueInstant(valueDeserializer)
        }

    }

    public companion object {
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

        private val recordersDirectory = File(RECORDERS_PATH).apply { mkdir() }

        //TODO: Change to ReadWriteMutex
        private val recorderUidMutex = Mutex()
        private var recorderUid: Long? = null

        private suspend fun getRecorderUid(): String =
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

        public suspend fun deleteAllRecordsFromDisk(): Boolean =
            withContext(Dispatchers.IO) {
                recordersDirectory.delete()
            }

    }
}