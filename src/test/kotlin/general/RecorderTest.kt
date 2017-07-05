package general

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.recording.MemoryRecorder
import io.kotlintest.specs.StringSpec
import org.tenkiv.coral.ValueInstant
import java.io.File
import java.time.Instant

class RecorderTest: StringSpec() {
    init{

        "Memory Recorder Test"{

            if(File("testJson.json").exists()){
                File("testJson.json").delete()
            }

            val deserailzer: (String) -> BinaryState = {
                if(it == BinaryState.On.toString()){
                    BinaryState.On
                }else{
                    BinaryState.Off}
            }
            val memoryRecorder = MemoryRecorder(1000,
                                                "testJson.json",
                                                deserailzer,
                                                PredictableDigitalSensor())
            Thread.sleep(5000)
            memoryRecorder.stop()
            println(memoryRecorder.getDataForTime(Instant.MIN, Instant.MAX))

            if(File("testJson.json").exists()){
                File("testJson.json").delete()
            }
        }
    }
}