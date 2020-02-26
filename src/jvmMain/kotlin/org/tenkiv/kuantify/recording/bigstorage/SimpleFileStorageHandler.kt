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

package org.tenkiv.kuantify.recording.bigstorage

import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.*
import kotlinx.coroutines.time.*
import kotlinx.serialization.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.recording.*
import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.charset.*
import java.nio.file.*
import java.time.*

public class SimpleFileStorageHandler<DT : DaqcData, GT : DaqcGate<DT>>(
    recorder: GateRecorder<DT, GT>,
    serializer: KSerializer<DT>
) : BigStorageHandler<DT, GT>(recorder, serializer) {
    private val uid = GlobalScope.async(Dispatchers.Daqc) { getRecorderUid() }
    private val directoryPath = GlobalScope.async(Dispatchers.Daqc) { "$RECORDERS_PATH/${uid.await()}" }
    private val directoryFile = GlobalScope.async(Dispatchers.Daqc) { File(directoryPath.await()).apply { mkdir() } }
    private val files = ArrayList<RecorderFile>()
    private val fileCreationBroadcaster = ConflatedBroadcastChannel<RecorderFile>()

    init {
        createRecordJob()
    }

    public override suspend fun getData(filter: StorageFilter<DT>): List<ValueInstant<DT>> =
        withContext(Dispatchers.Daqc) {
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
        result
    }

    public override suspend fun recordUpdate(update: ValueInstant<DT>) {
        files.lastOrNull()?.writeEntry(update) ?: TODO("throw exception")

        if (storageLength is StorageSamples.Number) files.forEach { it.samplesSinceCreation++ }
    }

    private fun storeEverythingInit() {
        files += RecorderFile(expiresIn = null)
    }

    private fun storeForDurationInit(duration: Duration) {
        launch {
            val fileCreationInterval = duration.dividedBy(10)
            val fileExpiresIn = duration + duration.dividedBy(9)
            while (isActive) {
                val newRecorderFile = RecorderFile(fileExpiresIn)
                files += newRecorderFile
                fileCreationBroadcaster.send(newRecorderFile)
                delay(fileCreationInterval)
            }
        }
    }

    private fun storeForNumSamplesInit(numSamples: Int) {
        launch {
            val fileCreationInterval = numSamples / 10
            val fileExpiresIn = (numSamples + numSamples / 9) + 1

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

    private fun createRecordJob() = launch(Dispatchers.Daqc) {
        when(val storageLength = storageLength) {
            is StorageDuration -> when(storageLength) {
                is StorageDuration.For -> storeForDurationInit(storageLength.duration)
                StorageDuration.Forever -> storeEverythingInit()
            }
            is StorageSamples -> when(storageLength) {
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

    private fun ValueInstant<DT>.toBase64(valueSerializer: KSerializer<DT>): String =
        Serialization.xdr.dump(ValueInstantSerializer(valueSerializer), this).encodeBase64()

    private fun ByteArray.toValueInstant(valueSerializer: KSerializer<DT>): ValueInstant<DT> =
        Serialization.xdr.load(ValueInstantSerializer(valueSerializer), this.decodeToString().decodeBase64Bytes())

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

        internal suspend fun writeEntry(entry: ValueInstant<DT>) {
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

        internal suspend fun readFromDisk(filter: (ValueInstant<DT>) -> Boolean): List<ValueInstant<DT>> {

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
                        val valueInstant = currentObjectBytes.toByteArray().toValueInstant(serializer)
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
                val lastUid =
                    recorderUid
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