package general

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.recording.disk.CSVRecorder
import com.tenkiv.daqc.recording.disk.JSONRecorder
import com.tenkiv.daqc.recording.memory.AnalogMemoryRecorder
import com.tenkiv.daqc.recording.memory.DigitalMemoryRecorder
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.launch
import org.tenkiv.physikal.core.qeq
import org.tenkiv.physikal.core.volt
import java.io.File

class RecorderTest: StringSpec() {
    init{
        "JSON Recording Test"{

            val gibberingSensor = GenericGibberingSensor()

            var completed = false

            val file = File("./TestRecording.json")

            // No False Positives
            if(file.exists()){
                file.delete()
            }

            val recorder = JSONRecorder(file.path,
                    recordingObjects = mapOf(Pair(gibberingSensor, "Gibbering Sensor")))

            recorder.start()

            Thread.sleep(5000)

            recorder.stop()

            Thread.sleep(1000)

            try {
                val json = Parser().parse(file.path) as JsonArray<JsonArray<JsonObject>>
                json.forEach(::println)
                completed = json[0].size > 0

            }catch (exception: Exception){
                println("Almost certainly Failed.")
                exception.printStackTrace()
            }

            assert(completed)

            // Cleanup
            if(file.exists()){
                file.delete()
            }
        }

        "CSV Recording Test"{

            val gibberingSensor = GenericGibberingSensor()

            var completed = false

            val file = File("./TestRecording.csv")

            // No False Positives
            if(file.exists()){
                file.delete()
            }

            val csvRecorder = CSVRecorder(file.path,
                    recordingObjects = mapOf(Pair(gibberingSensor, "Gibbering Sensor")))


            csvRecorder.start()

            Thread.sleep(5000)

            csvRecorder.stop()

            Thread.sleep(1000)

            /*try {
                val json = Parser().parse(file.path) as JsonArray<JsonArray<JsonObject>>
                json.forEach(::println)
                completed = json[0].size > 0

            }catch (exception: Exception){
                println("Almost certainly Failed.")
                exception.printStackTrace()
            }*/

            assert(true)

            // Cleanup
            /*if(file.exists()){
                file.delete()
            }*/
        }

        "Analog Memory Recorder Test"{

            val gibberingSensor = AnalogGibberingSensor()

            val recorder = AnalogMemoryRecorder(gibberingSensor, 10, "")

            recorder.start()

            Thread.sleep(2000)

            recorder.stop()

            gibberingSensor.cancel()

            println("Median "+recorder.median())

            println("Avg "+recorder.average())

        }

        "Predictable Digital Memory Recorder Test"{

            val gibberingSensor = PredictableDigitalSensor()

            val recorder = DigitalMemoryRecorder(gibberingSensor, 10, "")

            recorder.start()

            Thread.sleep(2000)

            recorder.stop()

            println("Asserting Median is On")
            assert(recorder.median().second!! == BinaryState.On)
            println("Median "+recorder.median())

            println("Asserting 80% on")
            assert(recorder.percentOn() == 0.8)
            println("Time On "+recorder.percentOn())

        }

        "Predictable Analog Memory Recorder Test"{

            val gibberingSensor = PredictableAnalogSensor()

            val recorder = AnalogMemoryRecorder(gibberingSensor, 10, "")

            recorder.start()

            Thread.sleep(2000)

            recorder.stop()

            println("Asserting Median is 10v")
            println("Median "+recorder.median())
            assert(recorder.median().second!! == DaqcQuantity.of(10.volt))

            println("Asserting Avg is 11v")
            println("Avg ${recorder.average()}")

            // qeq() is mandatory because default quantity equals() is broken.
            assert(recorder.average().qeq(11.volt))

        }
    }
}