package general

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.recording.MemoryRecorder
import com.tenkiv.daqc.recording.StorageDuration
import com.tenkiv.daqc.recording.StorageFrequency
import com.tenkiv.daqcThreadContext

import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.runBlocking
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.secondsSpan
import java.time.Instant

class RecorderTest: StringSpec() {

    val fileName = "TestJson.json"

    init{
        "Memory Recorder Test"{

            val deserializer: (String) -> BinaryState = {
                if(it == BinaryState.On.toString()){ BinaryState.On }else{ BinaryState.Off }
            }

            //val memoryRecorder = MemoryRecorder(dataDeserializer = deserializer,
            //        updatable = PredictableDigitalSensor())

            val memoryRecorder = MemoryRecorder(StorageFrequency.All,
                    StorageDuration.For(1L.secondsSpan),
                    StorageDuration.For(5L.secondsSpan),
                    deserializer,
                    DigitalGibberingSensor())

            Thread.sleep(50000)

            runBlocking{memoryRecorder.getDataForTime(Instant.MIN, Instant.MAX).consumeEach { println(it) }}
            //memoryRecorder.stop()
        }

        /*"Digital Memory Recorder Test"{

            if(File(fileName).exists()){
                File(fileName).delete()
            }

            val deserializer: (String) -> BinaryState = {
                if(it == BinaryState.On.toString()){ BinaryState.On }else{ BinaryState.Off }
            }

            val memoryRecorder = MemoryRecorder(1000,
                                                fileName,
                                                deserializer,
                                                PredictableDigitalSensor())
            Thread.sleep(5000)
            memoryRecorder.stop()

            val result = memoryRecorder.getDataForTime(Instant.MIN, Instant.MAX)

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

            val result = memoryRecorder.getDataForTime(Instant.MIN, Instant.MAX)

            println("Analog recording size was ${result.size} and value was ${result[5].value}")

            assert(result.size == 10 && result[5].value qeq 12.volt)

            if(File(fileName).exists()){
                File(fileName).delete()
            }
        }*/
    }

    class SomeClass(input: Input<ValueInstant<BinaryState>>){
        init {
            input.openNewCoroutineListener(daqcThreadContext) { println("$it") }
        }
    }
}