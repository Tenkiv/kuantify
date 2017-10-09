package org.tenkiv.daqc.recording

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.isOlderThan
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.*
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.channel.BinaryStateInput
import org.tenkiv.daqc.hardware.definitions.channel.BinaryStateOutput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityInput
import org.tenkiv.daqc.hardware.definitions.channel.QuantityOutput
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.charset.Charset
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import javax.measure.Quantity

typealias RecordedQuantityInput<Q> = RecordedUpdatable<DaqcQuantity<Q>, QuantityInput<Q>>
typealias RecordedBinaryStateInput = RecordedUpdatable<BinaryState, BinaryStateInput>
typealias RecordedQuantityOutput<Q> = RecordedUpdatable<DaqcQuantity<Q>, QuantityOutput<Q>>
typealias RecordedBinaryStateOutput = RecordedUpdatable<BinaryState, BinaryStateOutput>

//TODO: Move default parameter values in recorder creation function to constants
fun <T> Updatable<ValueInstant<T>>.newRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever,
        filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true },
        valueSerializer: (T) -> String,
        valueDeserializer: (String) -> T
): Recorder<T> =
        Recorder(this,
                storageFrequency,
                memoryDuration,
                diskDuration,
                filterOnRecord,
                valueSerializer,
                valueDeserializer)

fun Updatable<BinaryStateMeasurement>.newBinaryStateRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever,
        filterOnRecord: Recorder<BinaryState>.(ValueInstant<BinaryState>) -> Boolean = { true }
) =
        Recorder(this,
                storageFrequency,
                memoryDuration,
                diskDuration,
                filterOnRecord,
                valueSerializer = { "\"$it\"" },
                valueDeserializer = BinaryState.Companion::fromString)

//TODO: Add optional filterOnRecord parameter when supported by kotlin
inline fun <reified Q : Quantity<Q>> Updatable<QuantityMeasurement<Q>>.newQuantityRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever
): Recorder<DaqcQuantity<Q>> =
        newRecorder(storageFrequency,
                memoryDuration,
                diskDuration,
                filterOnRecord = { true },
                valueSerializer = { "\"$it\"" },
                valueDeserializer = DaqcQuantity.Companion::fromString)

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
                this.newRecorder(storageFrequency,
                        memoryDuration,
                        diskDuration,
                        filterOnRecord,
                        valueSerializer,
                        valueDeserializer)
        )

fun <U : Updatable<BinaryStateMeasurement>> U.pairWithNewBinStateRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever,
        filterOnRecord: Recorder<BinaryState>.(ValueInstant<BinaryState>) -> Boolean = { true }
) =
        RecordedUpdatable(
                this,
                newBinaryStateRecorder(storageFrequency,
                        memoryDuration,
                        diskDuration,
                        filterOnRecord)
        )

//TODO: Add optional filterOnRecord parameter when supported by kotlin
//TODO: Using type aliases here seems to crash the compiler, switch to type alias when that is fixed.
inline fun <reified Q : Quantity<Q>, U : Updatable<ValueInstant<DaqcQuantity<Q>>>>
        U.pairWithNewQuantityRecorder(
        storageFrequency: StorageFrequency = StorageFrequency.All,
        memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        diskDuration: StorageDuration = StorageDuration.Forever
) =
        RecordedUpdatable(this, newQuantityRecorder(storageFrequency, memoryDuration, diskDuration))

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


data class RecordedUpdatable<out T, out U : Updatable<ValueInstant<T>>>(val updatable: U,
                                                                        val recorder: Recorder<T>)

sealed class StorageFrequency {

    object All : StorageFrequency()

    data class Interval(val interval: Duration) : StorageFrequency()

    data class PerNumMeasurements(val number: Int) : StorageFrequency()
}

sealed class StorageDuration : Comparable<StorageDuration> {

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

/**
 * @param filterOnRecord filters the incoming [ValueInstant]s should return true if the recorder should store the
 * ValueInstant and false if it should not.
 * @param valueSerializer returned String will be stored in a JSON file and should be a compliant JSON String.
 */
class Recorder<out T> internal constructor(
        updatable: Updatable<ValueInstant<T>>,
        val storageFrequency: StorageFrequency = StorageFrequency.All,
        val memoryDuration: StorageDuration = StorageDuration.For(30L.secondsSpan),
        val diskDuration: StorageDuration = StorageDuration.None,
        private val filterOnRecord: Recorder<T>.(ValueInstant<T>) -> Boolean = { true },
        private val valueSerializer: (T) -> String,
        private val valueDeserializer: (String) -> T
) {

    private val uid = getRecorderUid()
    private val directoryPath = async(daqcThreadContext) { "$RECORDERS_PATH/${uid.await()}" }
    private val directoryFile = async(daqcThreadContext) { File(directoryPath.await()).apply { mkdir() } }

    private val _dataInMemory = ArrayList<ValueInstant<T>>()
    private val _reversedDataInMemory = _dataInMemory.asReversed()

    private val files = ArrayList<RecorderFile>()
    private val reversedFiles = files.asReversed()

    private val receiveChannel = updatable.broadcastChannel.openSubscription()


    private val recordJob = launch(daqcThreadContext) {
        if (diskDuration is StorageDuration.Forever)
            files += RecorderFile(null)

        if (diskDuration is StorageDuration.For)
            launch(coroutineContext) {
                val fileCreationInterval = diskDuration.duration.dividedBy(10)
                val fileExpiresIn = diskDuration.duration + diskDuration.duration.dividedBy(9)
                while (isActive) {
                    files += RecorderFile(fileExpiresIn)
                    delay(fileCreationInterval.toMillis())
                }
            }


        if (storageFrequency is StorageFrequency.Interval)
            while (isActive) {
                delay(storageFrequency.interval.toMillis())
                val latestUpdate = updatable.broadcastChannel.valueOrNull
                if (latestUpdate != null)
                    recordUpdate(latestUpdate)
                cleanMemory()
            }
        else {
            var numUnstoredMeasurements = 0

            receiveChannel.consumeEach { update ->
                if (storageFrequency is StorageFrequency.All)
                    recordUpdate(update)
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

    /**
     * Gets the data currently stored in RAM by this recorder. The returned list can not be modified,
     * to get an updated list of data in memory, call this function again.
     */
    //TODO: Make this return truly immutable list.
    fun getDataInMemory(): List<ValueInstant<T>> = ArrayList(_dataInMemory)

    //TODO: Make this return a truly immutable list using a builder
    fun getAllData(): Deferred<List<ValueInstant<T>>> = async(daqcThreadContext) {
        if (memoryDuration >= diskDuration)
            getDataInMemory()
        else {
            val data = ArrayList<ValueInstant<T>>()
            reversedFiles.forEach {
                data.addAll(it.readFromDisk { true })
            }
            data
        }
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
                else if (diskDuration > memoryDuration) {
                    getDataFromDisk(filterFun)
                } else
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
        reversedFiles.forEach { result.addAll(it.readFromDisk(filter)) }
        return result
    }

    private suspend fun recordUpdate(update: ValueInstant<T>) {
        if (filterOnRecord(update)) {
            if (memoryDuration !is StorageDuration.None)
                _dataInMemory += update
            files.lastOrNull()?.writeEntry(update)
        }
    }

    private suspend fun cleanMemory() {
        if (memoryDuration is StorageDuration.For) {
            val iterator = _reversedDataInMemory.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().instant.isOlderThan(memoryDuration.duration))
                    iterator.remove()
                else
                    break
            }
        }
    }

    // This has to be an extension function in order to keep out variance in Recorder class generic
    private suspend fun RecorderFile.writeEntry(entry: ValueInstant<T>) {
        val jsonObjectString =
                "{\"$INSTANT_KEY\":${entry.instant.toEpochMilli()}," +
                        "\"$VALUE_KEY\":${valueSerializer(entry.value)}}"
        writeJsonBuffer(jsonObjectString)
    }

    //▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
    //   ⎍⎍⎍⎍⎍⎍⎍⎍   ஃ Inner class ஃ   ⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍⎍    //
    //▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬//
    private inner class RecorderFile internal constructor(val expiresIn: Duration?) {

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

        init {
            if (expiresIn != null)
                launch(daqcThreadContext) {
                    delay(expiresIn.toMillis())
                    files.remove(this@RecorderFile)

                    fileChannel.await().close()
                    file.await().delete()
                }

        }

        internal suspend fun writeJsonBuffer(jsonObjectString: String,
                                             charset: Charset = Charset.defaultCharset()) {
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

        private suspend fun writeToFile(string: String,
                                        charset: Charset = Charset.defaultCharset()) {
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

        internal suspend fun readFromDisk(filter: (ValueInstant<T>) -> Boolean): List<ValueInstant<T>> {
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

                    if (numUnclosedBraces > 1) {
                        currentObject += charByte
                    }

                    if (!inString && charByte == CLOSE_BRACE) {
                        numUnclosedBraces--
                        if (numUnclosedBraces == 0)
                            return@readFromDisk complyingObjects

                        val valueInstant = jacksonMapper
                                .readValue<PrimitiveValueInstant>(currentObject.toByteArray())
                                .toValueInstant(valueDeserializer)
                        if (filter(valueInstant)) {
                            complyingObjects += valueInstant
                        }
                        currentObject.clear()

                    }

                    previousCharByte = charByte
                }
                position += 100
                buffer = ByteBuffer.allocate(100)

            }
            return complyingObjects
        }
    }


    companion object {
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

        private val jacksonMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

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