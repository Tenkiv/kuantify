package general

import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import org.tenkiv.coral.at
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.recording.*
import java.time.Instant

class RecorderTest : StringSpec() {

    init {
        "Memory Recorder Test"{

            val deserializer: (String) -> BinaryState = {
                if (it == BinaryState.On.toString()) {
                    BinaryState.On
                } else {
                    BinaryState.Off
                }
            }

            val testData = listOf(
                    0.at(Instant.MAX),
                    1.at(Instant.now()),
                    2.at(Instant.now()),
                    3.at(Instant.now()),
                    4.at(Instant.now()),
                    5.at(Instant.MAX)
            )

            Thread.sleep(100)

            assert(testData.getDataInRange(Instant.MIN..Instant.now()).size == 4)

            val recorders = ArrayList<Recorder<BinaryState>>()
            for (i in 0..20) {
                recorders.add(
                        DigitalGibberingSensor().newBinaryStateRecorder(storageFrequency = StorageFrequency.All,
                                memoryDuration = StorageDuration.None,
                                diskDuration = StorageDuration.Forever)
                )
            }

            Thread.sleep(10000)

            runBlocking {
                recorders.forEach {
                    println("Fetching Data")
                    //it.stop(false)
                    println(it.getAllData().await())
                }
            }

            Thread.sleep(10000)
        }
    }
}