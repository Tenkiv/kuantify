package general

import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.secondsSpan
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.hardware.definitions.channel.Input
import org.tenkiv.daqc.recording.Recorder
import org.tenkiv.daqc.recording.StorageDuration
import org.tenkiv.daqc.recording.StorageFrequency
import org.tenkiv.daqcThreadContext
import java.time.Instant

class RecorderTest : StringSpec() {

    val fileName = "TestJson.json"

    init {
        "Memory Recorder Test"{

            val deserializer: (String) -> BinaryState = {
                if (it == BinaryState.On.toString()) {
                    BinaryState.On
                } else {
                    BinaryState.Off
                }
            }

            //val memoryRecorder = Recorder(dataDeserializer = deserializer,
            //        updatable = PredictableDigitalSensor())

            val memoryRecorder = Recorder(StorageFrequency.All,
                    StorageDuration.For(1L.secondsSpan),
                    StorageDuration.For(5L.secondsSpan),
                    DigitalGibberingSensor(),
                    deserializer)

            Thread.sleep(50000)

            runBlocking { memoryRecorder.getDataInRange(Instant.MIN, Instant.MAX).consumeEach { println(it) } }
            //memoryRecorder.stop()
        }

        /*"Digital Memory Recorder Test"{

            if(File(fileName).exists()){
                File(fileName).delete()
            }

            val deserializer: (String) -> BinaryState = {
                if(it == BinaryState.On.toString()){ BinaryState.On }else{ BinaryState.Off }
            }

            val memoryRecorder = Recorder(1000,
                                                fileName,
                                                deserializer,
                                                PredictableDigitalSensor())
            Thread.sleep(5000)
            memoryRecorder.stop()

            val result = memoryRecorder.getDataInRange(Instant.MIN, Instant.MAX)

            println("Digital recording size was ${result.size} and value was ${result[5].value}")

            assert(result.size == 10 && result[5].value == BinaryState.On)

            if(File(fileName).exists()){
                File(fileName).delete()
            }
        }

        "Analog Memory Recorder Test"{

            if(File(fileName).exists()){
                File(fileName).delete()
            }

            val memoryRecorder = getDaqcValueRecorder(10000,
                    fileName,
                    PredictableAnalogSensor())
            Thread.sleep(5000)
            memoryRecorder.stop()

            val result = memoryRecorder.getDataInRange(Instant.MIN, Instant.MAX)

            println("Analog recording size was ${result.size} and value was ${result[5].value}")

            assert(result.size == 10 && result[5].value qeq 12.volt)

            if(File(fileName).exists()){
                File(fileName).delete()
            }
        }*/
    }

    class SomeClass(input: Input<ValueInstant<BinaryState>>) {
        init {
            input.openNewCoroutineListener(daqcThreadContext) { println("$it") }
        }
    }
}