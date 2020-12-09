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

package kuantify.recording.bigstorage

import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.*
import org.tenkiv.coral.*
import kuantify.*
import kuantify.data.*
import kuantify.gate.*
import kuantify.lib.*
import kuantify.recording.*
import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.charset.*
import java.nio.file.*
import kotlin.time.*

public class SimpleFileStorageHandler<DataT : DaqcData, ChannelT : DaqcChannel<DataT>>(
    recorder: Recorder<DataT, ChannelT>,
    serializer: KSerializer<DataT>
) : BigStorageHandler<DataT, ChannelT>(recorder, serializer) {
    private val uid = GlobalScope.async(Dispatchers.Daqc) { getRecorderUid() }
    private val directoryPath = GlobalScope.async(Dispatchers.Daqc) { "$RECORDERS_PATH/${uid.await()}" }
    private val directoryFile = GlobalScope.async(Dispatchers.Daqc) { File(directoryPath.await()).apply { mkdir() } }
    private val files = ArrayList<RecorderFile>()
    private val newFileNotifier = Channel<Unit>()

    init {
        createRecordJob()
    }

    public override suspend fun getData(filter: StorageFilter<DataT>): List<ValueInstant<DataT>> =
        withContext(Dispatchers.Daqc) {
            //TODO: Change to immutable list using builder
            val result = ArrayList<ValueInstant<DataT>>()
            val currentFiles: List<RecorderFile> = ArrayList(files)
            val buffer = ArrayList<ValueInstant<DataT>>()

            val bufferJob = launch(Dispatchers.Daqc) {
                newFileNotifier.receive()
                channel.onEachUpdate { value ->
                    buffer += value
                }
            }

            currentFiles.forEach {
                if (it.isOpen.await()) result.addAll(it.readFromDisk(filter))
            }

            result.addAll(buffer.filter(filter))

            bufferJob.cancel()
            result
        }

    public override suspend fun recordUpdate(update: ValueInstant<DataT>) {
        files.lastOrNull()?.writeEntry(update) ?: throw IllegalStateException("Cannot record update without a file.")

        if (storageLength is StorageSamples.Number) files.forEach { it.samplesSinceCreation++ }
    }

    private fun storeEverythingInit() {
        files += RecorderFile(expiresIn = null)
    }

    private fun CoroutineScope.storeForDurationInit(duration: Duration) {
        launch {
            val fileCreationInterval = duration / 10
            val fileExpiresIn = duration + duration / 9
            while (isActive) {
                val newRecorderFile = RecorderFile(fileExpiresIn)
                files += newRecorderFile
                newFileNotifier.send(Unit)
                delay(fileCreationInterval)
            }
        }
    }

    private fun CoroutineScope.storeForNumSamplesInit(numSamples: Int32) {
        launch {
            val fileCreationInterval = numSamples / 10
            val fileExpiresIn = (numSamples + numSamples / 9) + 1

            files += RecorderFile(fileExpiresIn)

            channel.onEachUpdate {
                if (files.last().samplesSinceCreation == fileCreationInterval) {
                    files += RecorderFile(fileExpiresIn)
                    newFileNotifier.send(Unit)
                }
            }

        }
    }

    private fun createRecordJob() = launch(Dispatchers.Daqc) {
        when (val storageLength = storageLength) {
            is StorageDuration -> when (storageLength) {
                is StorageDuration.For -> storeForDurationInit(storageLength.duration)
                StorageDuration.Forever -> storeEverythingInit()
            }
            is StorageSamples -> when (storageLength) {
                is StorageSamples.Number -> storeForNumSamplesInit(storageLength.numSamples)
                StorageSamples.All -> storeEverythingInit()
            }
        }
    }

    public override suspend fun cancel(shouldDeleteData: Boolean) {
        val stoppingFiles = ArrayList<Job>()
        files.forEach { file ->
            stoppingFiles += launch { file.cancel() }
        }
        if (shouldDeleteData) {
            withContext(Dispatchers.IO) {
                directoryFile.await().delete()
            }
        }

        stoppingFiles.joinAll()
    }

    private fun ValueInstant<DataT>.toBase64(valueSerializer: KSerializer<DataT>): String =
        Serialization.cbor.encodeToByteArray(ValueInstantSerializer(valueSerializer), this).encodeBase64()

    private fun ByteArray.toValueInstant(valueSerializer: KSerializer<DataT>): ValueInstant<DataT> =
        Serialization.cbor.decodeFromByteArray(
            ValueInstantSerializer(valueSerializer),
            this.decodeToString().decodeBase64Bytes()
        )

    //▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ஃ Inner class ஃ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
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

        internal constructor(expiresAfterNumSamples: Int32?) {
            if (expiresAfterNumSamples != null) {
                launch(Dispatchers.Daqc) {

                    channel.valueFlow.takeWhile {
                        samplesSinceCreation < expiresAfterNumSamples
                    }.collect()

                    suicide()
                }
            }
        }

        internal suspend fun writeEntry(entry: ValueInstant<DataT>) {
            writeJsonBuffer("\"${entry.toBase64(serializer)}\"")
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

        internal suspend fun readFromDisk(filter: (ValueInstant<DataT>) -> Boolean): List<ValueInstant<DataT>> {

            val channel = fileChannel.await()
            val currentObjectBytes = ByteArrayOutputStream()
            val complyingObjects = ArrayList<ValueInstant<DataT>>()
            val buffer = ByteBuffer.allocate(100)
            var position = 0L

            while (channel.isOpen) {
                mutex.withLock { channel.aRead(buffer, position) }

                buffer.array().forEach { byte ->
                    if (byte == ARRAY_END.toByte()) return@readFromDisk complyingObjects
                    if (byte == VALUE_SEPERATOR.toByte()) {
                        val valueInstant = currentObjectBytes.toByteArray().toValueInstant(serializer)
                        if (filter(valueInstant)) complyingObjects += valueInstant
                        currentObjectBytes.reset()
                    } else {
                        currentObjectBytes.write(byte.toInt32())
                    }
                }
                position += 100
                buffer.clear()
            }
            return complyingObjects

        }

    }

    public companion object {
        private const val ARRAY_END = '.'
        private const val VALUE_SEPERATOR = ','

        private const val RECORDERS_PATH = "recorders"

        private val recordersDirectory = File(RECORDERS_PATH).apply { mkdir() }

        //TODO: Change to ReadWriteMutex
        private val recorderUidMutex = Mutex()
        private var recorderUid: Int64? = null

        private suspend fun getRecorderUid(): String =
            recorderUidMutex.withLock {
                val lastUid =
                    recorderUid
                val thisUid: Int64

                if (lastUid != null) {
                    thisUid = lastUid + 1
                    recorderUid = thisUid
                } else {
                    thisUid = recordersDirectory.listFiles()
                        ?.map { it.name.toLongOrNull() }
                        ?.requireNoNulls()
                        ?.maxOrNull()?.plus(1) ?: 1L
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