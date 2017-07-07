package general

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.recording.MemoryRecorder
import com.tenkiv.daqc.recording.getDaqcValueRecorder
import io.kotlintest.specs.StringSpec
import org.tenkiv.coral.ValueInstant
import org.tenkiv.physikal.core.qeq
import org.tenkiv.physikal.core.volt
import java.io.File
import java.time.Instant

class RecorderTest: StringSpec() {

    val fileName = "TestJson.json"

    init{

        "Digital Memory Recorder Test"{

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
        }
    }
}