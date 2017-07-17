package tekdaqc

import io.kotlintest.specs.StringSpec

class TekdaqcTest: StringSpec(){

    init{

        /*"Crossover-Tekdaqc Test" {
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
        }*/
    }

    /*suspend fun executeBoardCommands(board: TekdaqcBoard){
        println("Executing Board Commands")
        board.connect(LineNoiseFrequency.AccountFor(60.hertz),NetworkProtocol.TELNET)

        board.tekdaqc.addListener(object: IMessageListener{
            override fun onDigitalInputDataReceived(tekdaqc: ATekdaqc?, data: DigitalInputData?) {}

            override fun onDebugMessageReceived(tekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Debug ${message.toString()}")
            }

            override fun onErrorMessageReceived(tekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Error ${message.toString()}")
            }

            override fun onAnalogInputDataReceived(tekdaqc: ATekdaqc?, data: AnalogInputCountData?) {
                println("Analog ${data?.data?.toString()}")
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
    }*/
}