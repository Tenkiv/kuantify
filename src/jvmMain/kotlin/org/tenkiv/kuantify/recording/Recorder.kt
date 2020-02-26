/*
 * Copyright 2020 Tenkiv, Inc.
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

import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.*
import kotlinx.coroutines.time.*
import kotlinx.io.ByteArrayOutputStream
import kotlinx.serialization.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.recording.bigstorage.*
import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.charset.*
import java.nio.file.*
import java.time.*
import kotlin.coroutines.*

public typealias RecordingFilter<DT, GT> = Recorder<DT, GT>.(ValueInstant<DT>) -> Boolean
internal typealias StorageFilter<DT> = (ValueInstant<DT>) -> Boolean

public fun <DT : DaqcData, GT : DaqcGate<DT>> CoroutineScope.Recorder(
    gate: GT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageLength,
    filterOnRecord: RecordingFilter<DT, GT> = { true }
): MemoryRecorder<DT, GT> = MemoryRecorder(this, gate, storageFrequency, memoryStorageLength, filterOnRecord)

public fun <DT : DaqcData, GT : DaqcGate<DT>> CoroutineScope.Recorder(
    gate: GT,
    storageFrequency: StorageFrequency,
    bigStorageLength: StorageLength,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DT, GT>,
    filterOnRecord: RecordingFilter<DT, GT> = { true }
): BigStorageRecorder<DT, GT> =
    BigStorageRecorder(this, gate, storageFrequency, bigStorageLength, bigStorageHandlerCreator, filterOnRecord)

public fun <DT : DaqcData, GT : DaqcGate<DT>> CoroutineScope.Recorder(
    gate: GT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageSamples,
    bigStorageLength: StorageSamples,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DT, GT>,
    filterOnRecord: RecordingFilter<DT, GT> = { true }
): CombinedRecorder<DT, GT> = CombinedRecorder(
    this,
    gate,
    storageFrequency,
    memoryStorageLength,
    bigStorageLength,
    bigStorageHandlerCreator,
    filterOnRecord
)

public fun <DT : DaqcData, GT : DaqcGate<DT>> CoroutineScope.Recorder(
    gate: GT,
    storageFrequency: StorageFrequency,
    memoryStorageLength: StorageDuration,
    bigStorageLength: StorageDuration,
    bigStorageHandlerCreator: BigStorageHandlerCreator<DT, GT>,
    filterOnRecord: RecordingFilter<DT, GT> = { true }
): CombinedRecorder<DT, GT> = CombinedRecorder(
    this,
    gate,
    storageFrequency,
    memoryStorageLength,
    bigStorageLength,
    bigStorageHandlerCreator,
    filterOnRecord
)

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
     * Keep a specific number of samples in memory or on disk.
     *
     * @param numSamples The number of samples to keep in memory or on disk.
     */
    public data class Number(val numSamples: Int) : StorageSamples() {

        public override fun compareTo(other: StorageSamples): Int =
            when (other) {
                is All -> -1
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
     * Keep the data for a specified duration.
     *
     * @param duration The [Duration] with which to keep the data.
     */
    public data class For(val duration: Duration) : StorageDuration() {

        public override fun compareTo(other: StorageDuration): Int =
            when (other) {
                is Forever -> -1
                is For -> duration.compareTo(other.duration)
            }
    }
}

public interface Recorder<out DT : DaqcData, out GT : DaqcGate<DT>> : CoroutineScope {
    public val gate: GT
    public val storageFrequency: StorageFrequency
    public val memoryStorageLength: StorageLength?
    public val bigStorageLength: StorageLength?

    public fun getDataInMemory(): List<ValueInstant<DT>>

    public suspend fun getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<DT>>

    public suspend fun getAllData(): List<ValueInstant<DT>>

    public suspend fun cancel(deleteBigStorage: Boolean = false)
}

internal fun <DT : DaqcData, GT : DaqcGate<DT>> Recorder<DT, GT>.createRecordJob(
    memoryHandler: MemoryHandler<DT>?,
    bigStorageHandler: BigStorageHandler<DT, GT>?,
    filterOnRecord: RecordingFilter<DT, GT>
) = launch {
    when(val storageFrequency = storageFrequency) {
        StorageFrequency.All -> gate.updateBroadcaster.openSubscription().consumeEach { update ->
            recordUpdate(update, memoryHandler, bigStorageHandler, filterOnRecord)
        }
        is StorageFrequency.Interval -> while (isActive) {
                delay(storageFrequency.interval)
                recordUpdate(gate.getValue(), memoryHandler, bigStorageHandler, filterOnRecord)
            }
        is StorageFrequency.PerNumMeasurements -> gate.updateBroadcaster.openSubscription().consumeEach { update ->
                var numUnstoredMeasurements = 0
                numUnstoredMeasurements++
                if (numUnstoredMeasurements == storageFrequency.number) {
                    recordUpdate(update, memoryHandler, bigStorageHandler, filterOnRecord)
                    numUnstoredMeasurements = 0
                }
            }
    }
}

private suspend fun <DT : DaqcData, GT : DaqcGate<DT>> Recorder<DT, GT>.recordUpdate(
    update: ValueInstant<DT>,
    memoryHandler: MemoryHandler<DT>?,
    bigStorageHandler: BigStorageHandler<DT, GT>?,
    filter: RecordingFilter<DT, GT>
) {
    if (filter(update)) {
        memoryHandler?.recordUpdate(update)
        bigStorageHandler?.recordUpdate(update)
        memoryHandler?.cleanMemory()
    }
}

/**
 * Recorder to store data either in memory, on disk, or both depending on certain parameters.
 */
public class GateRecorder<out DT : DaqcData, out GT : DaqcGate<DT>> : CoroutineScope {

    public val gate: GT
    public val storageFrequency: StorageFrequency
    public val memoryStorageLength: StorageLength
    public val diskStorageLength: StorageLength
    private val bigStorageHandler: BigStorageHandler<DT, GT>?
    private val filterOnRecord: RecordingFilter<DT, GT>
    private val valueSerializer: KSerializer<DT>?

    private val job: Job
    public override val coroutineContext: CoroutineContext

    private val receiveChannel: ReceiveChannel<ValueInstant<DT>>

    private val uid = GlobalScope.async(Dispatchers.Daqc) { getRecorderUid() }
    private val directoryPath = GlobalScope.async(Dispatchers.Daqc) { "$RECORDERS_PATH/${uid.await()}" }
    private val directoryFile = GlobalScope.async(Dispatchers.Daqc) { File(directoryPath.await()).apply { mkdir() } }

    private val _dataInMemory = ArrayList<ValueInstant<DT>>()

    private val files = ArrayList<RecorderFile>()

    private val fileCreationBroadcaster = ConflatedBroadcastChannel<RecorderFile>()

    /**
     * Recorder to store data either in memory or on disk depending on certain parameters.
     *
     * @param gate The [Trackable] to monitor for samples.
     * @param storageFrequency Determines how frequently data is stored to the [GateRecorder].
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
        gate: GT,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(memoryDurationDefault),
        diskDuration: StorageDuration = StorageDuration.None,
        filterOnRecord: RecordingFilter<DT, GT> = { true },
        valueSerializer: KSerializer<DT>
    ) {
        this.job = Job(scope.coroutineContext[Job])
        this.coroutineContext = scope.coroutineContext + job

        this.gate = gate
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = memoryDuration
        this.diskStorageLength = diskDuration
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = valueSerializer

        this.receiveChannel = gate.updateBroadcaster.openSubscription()
    }

    /**
     * Recorder to store data either in memory or on disk depending on certain parameters.
     *
     * @param gate The [Trackable] to monitor for samples.
     * @param storageFrequency Determines how frequently data is stored to the [GateRecorder].
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
        gate: GT,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        numSamplesMemory: StorageSamples = StorageSamples.Number(100),
        numSamplesDisk: StorageSamples = StorageSamples.None,
        filterOnRecord: RecordingFilter<DT, GT> = { true },
        valueSerializer: KSerializer<DT>
    ) {
        this.job = Job(scope.coroutineContext[Job])
        this.coroutineContext = scope.coroutineContext + job

        this.gate = gate
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = numSamplesMemory
        this.diskStorageLength = numSamplesDisk
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = valueSerializer

        this.receiveChannel = gate.updateBroadcaster.openSubscription()
    }

    /**
     * Recorder to store data either in memory or on disk depending on certain parameters.
     *
     * @param gate The [Trackable] to monitor for samples.
     * @param storageFrequency Determines how frequently data is stored to the [GateRecorder].
     * @param memoryStorageLength Determines how long samples are stored in memory.
     * @param filterOnRecord filters the incoming [ValueInstant]s should return true if the recorder should store the
     * ValueInstant and false if it should not.
     */
    @PublishedApi
    internal constructor(
        scope: CoroutineScope,
        gate: GT,
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryStorageLength: StorageLength = StorageSamples.Number(100),
        filterOnRecord: RecordingFilter<DT, GT> = { true }
    ) {
        this.job = Job(scope.coroutineContext[Job])
        this.coroutineContext = scope.coroutineContext + job

        this.gate = gate
        this.storageFrequency = storageFrequency
        this.memoryStorageLength = memoryStorageLength
        this.diskStorageLength = StorageSamples.None
        this.filterOnRecord = filterOnRecord
        this.valueSerializer = null

        this.receiveChannel = gate.updateBroadcaster.openSubscription()
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
    public fun getDataInMemory(): List<ValueInstant<DT>> = ArrayList(_dataInMemory)

    /**
     * Gets all data both in heap memory and disk. If disk data exists the function will suspend until data is restored
     * to heap memory.
     *
     * @return A [List] of [ValueInstant]s of all the data sent by the Recorder's [Trackable].
     */
    public suspend fun getAllData(): List<ValueInstant<DT>> =
        if (memoryStorageLength >= diskStorageLength) {
            getDataInMemory()
        } else {
            bigStorageHandler.getData { true }
        }

    /**
     * Gets all the data between two points in time denoted by [Instant]s. If disk data is in range the function will
     * suspend until data is restored to heap memory.
     *
     * @param instantRange The range of time over which to get data.
     * @return A [List] of [ValueInstant]s stored by the recorder within the [ClosedRange].
     */
    public suspend fun getDataInRange(instantRange: ClosedRange<Instant>): List<ValueInstant<DT>> =
        withContext(Dispatchers.Daqc) {
            val oldestRequested = instantRange.start

            val filterFun: StorageFilter<DT> = { it.instant in instantRange }

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
     * Performs required cleanups and stops the recorder.
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
    private suspend fun getDataFromDisk(filter: StorageFilter<DT>): List<ValueInstant<DT>> {
        //TODO: Change to immutable list using builder
        val result = ArrayList<ValueInstant<DT>>()
        val currentFiles: List<RecorderFile> = ArrayList(files)

        val buffer = ArrayList<ValueInstant<DT>>()

        val bufferJob = launch(Dispatchers.Daqc) {
            fileCreationBroadcaster.openSubscription().receive()
            gate.updateBroadcaster.consumeEach { value ->
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

    private suspend fun recordUpdate(update: ValueInstant<DT>) {
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
    private suspend fun RecorderFile.writeEntry(entry: ValueInstant<DT>) {
        if (valueSerializer != null) {
            writeJsonBuffer("\"${entry.toBase64(valueSerializer)}\"")
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
                val receiveChannel = gate.updateBroadcaster.openSubscription()
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
                recordUpdate(gate.getValue())
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

    private fun ValueInstant<DT>.toBase64(valueSerializer: KSerializer<DT>): String =
        Serialization.xdr.dump(ValueInstantSerializer(valueSerializer), this).encodeBase64()

    private fun ByteArray.toValueInstant(valueSerializer: KSerializer<DT>): ValueInstant<DT> =
        Serialization.xdr.load(ValueInstantSerializer(valueSerializer), this.decodeToString().decodeBase64Bytes())

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
        internal val fileChannel = async(Dispatchers.IO) {
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
                    val receiveChannel = gate.updateBroadcaster.openSubscription()

                    while (samplesSinceCreation < expiresAfterNumSamples) {
                        receiveChannel.receive()
                    }

                    suicide()
                }
            }
        }

        internal suspend fun writeJsonBuffer(base64String: String) {
            val string = "$base64String,$ARRAY_END"
            writeToFile(string)

            arrayLastPosition += string.length - 1
        }

        private suspend fun writeToFile(
            string: String,
            charset: Charset = Charset.defaultCharset()
        ) {
            val buffer = ByteBuffer.wrap(string.encodeToByteArray())
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

        internal suspend fun readFromDisk(filter: (ValueInstant<DT>) -> Boolean): List<ValueInstant<DT>> {
            if (valueSerializer != null) {
                val channel = fileChannel.await()
                val currentObjectBytes = ByteArrayOutputStream()
                val complyingObjects = ArrayList<ValueInstant<DT>>()
                val buffer = ByteBuffer.allocate(100)
                var position = 0L

                while (channel.isOpen) {
                    mutex.withLock { channel.aRead(buffer, position) }

                    buffer.array().forEach { byte ->
                        if (byte == ARRAY_END.toByte()) return@readFromDisk complyingObjects
                        if (byte == VALUE_SEPERATOR.toByte()) {
                            val valueInstant = currentObjectBytes.toByteArray().toValueInstant(valueSerializer)
                            if (filter(valueInstant)) complyingObjects += valueInstant
                            currentObjectBytes.reset()
                        } else {
                            currentObjectBytes.write(byte.toInt())
                        }
                    }
                    position += 100
                    buffer.clear()
                }
                return complyingObjects
            } else {
                throw IllegalStateException("valueDeserializer cannot be null if recorder is using disk for storage.")
            }
        }

    }

    public companion object {
        @PublishedApi
        internal val memoryDurationDefault = 30L.secondsSpan

        private const val ARRAY_END = '.'
        private const val VALUE_SEPERATOR = ','

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
                        ?.map { it.name.toLongOrNull() }
                        ?.requireNoNulls()
                        ?.max()?.plus(1) ?: 1L
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