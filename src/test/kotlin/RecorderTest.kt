import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.tenkiv.daqc.recording.AnalogMemoryRecorder
import com.tenkiv.daqc.recording.JSONRecorder
import io.kotlintest.specs.StringSpec
import java.io.File

/**
 * Created by tenkiv on 5/15/17.
 */
class RecorderTest: StringSpec() {
    init{
        "JSON Recording Test"{

            val gibberingSensor = GenericGibberingSensor()

            var completed = false

            val file = File("./TestRecording.json")

            //No False Positives
            if(file.exists()){
                file.delete()
            }

            val recorder = JSONRecorder(file.path,
                    recordingObjects = mapOf(Pair(gibberingSensor ,"Gibbering Sensor")))

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
            }

            assert(completed)

            // Cleanup
            if(file.exists()){
                file.delete()
            }
        }

        "InMemory Recorder Test"{

            val gibberingSensor = AnalogGibberingSensor()

            val recorder = AnalogMemoryRecorder(gibberingSensor,10,"")

            recorder.start()

            Thread.sleep(10000)

            recorder.stop()

            println("Median "+recorder.median())

            println("Avg "+recorder.average())

        }
    }
}