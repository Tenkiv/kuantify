package tekdaqc

import com.tenkiv.daqc.networking.NetworkProtocol
import com.tenkiv.tekdaqc.TekdaqcBoard
import com.tenkiv.tekdaqc.TekdaqcLocator
import com.tenkiv.tekdaqc.communication.data_points.AnalogInputCountData
import com.tenkiv.tekdaqc.communication.data_points.DigitalInputData
import com.tenkiv.tekdaqc.communication.message.ABoardMessage
import com.tenkiv.tekdaqc.communication.message.IMessageListener
import com.tenkiv.tekdaqc.hardware.ATekdaqc
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run

/**
 * Created by tenkiv on 6/5/17.
 */

class TekdaqcTest: StringSpec(){

    init{
        "Tekdaqc Location Test"{
            var success = false
            val tekdaqcLocator = TekdaqcLocator()

            launch(CommonPool){
                tekdaqcLocator.search()
                tekdaqcLocator.broadcastChannel.consumeEach {
                    it.forEach { board -> println("Discovery Test Located: "+board.inetAddr); success = true }
                }
                tekdaqcLocator.broadcastChannel.close()
            }

            Thread.sleep(10000)

            assert(success)
        }

        "Crossover-Tekdaqc Test" {
            val tekdaqcLocator = TekdaqcLocator()

            launch(CommonPool){

                tekdaqcLocator.broadcastChannel.consumeEach {
                    println("Got Something: $it")
                    it.forEach {
                        println(it.tekdaqc.serialNumber)
                        if(it.tekdaqc.serialNumber == "00000000000000000000000000000017"){
                            executeBoardCommands(it)
                            tekdaqcLocator.stop()
                        }
                    }
                }
            }

            tekdaqcLocator.search()
            Thread.sleep(10000)

            assert(true)
        }
    }

    suspend fun executeBoardCommands(board: TekdaqcBoard){
        println("Executing Board Commands")
        board.connect(NetworkProtocol.TELNET)

        board.tekdaqc.addListener(object: IMessageListener{
            override fun onDigitalInputDataReceived(tekdaqc: ATekdaqc?, data: DigitalInputData?) {}

            override fun onDebugMessageReceived(tekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Debug ${message.toString()}")
            }

            override fun onErrorMessageReceived(tekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Error ${message.toString()}")
            }

            override fun onAnalogInputDataReceived(tekdaqc: ATekdaqc?, data: AnalogInputCountData?) {
            }

            override fun onCommandDataMessageReceived(tekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Command ${message.toString()}")
            }

            override fun onStatusMessageReceived(tekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Status ${message.toString()}")
            }

            override fun onDigitalOutputDataReceived(tekdaqc: ATekdaqc?, data: BooleanArray?) {
            }

        })

        launch(CommonPool) {
            board.analogInputs[0].broadcastChannel.consumeEach {
                println("New Value: $it")
            }
        }

        board.analogInputs[0].activate()

        board.tekdaqc.sample(100)
    }
}