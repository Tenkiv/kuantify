package tekdaqc

class TekdaqcTest {

    init{

        //val tekdaqcLoc = TekdaqcLocator()

        //Locator.addDeviceLocator(tekdaqcLoc)

        /*"Crossover-Tekdaqc Test" {
            val tekdaqcLocator = TekdaqcLocator()

            launch(CommonPool){

                tekdaqcLocator.broadcastChannel.consumeEach {
                    println("Got Something: $it")
                    it.forEach {
                        println(it.wrappedTekdaqc.serialNumber)
                        if(it.wrappedTekdaqc.serialNumber == "00000000000000000000000000000017"){
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

    /*suspend fun executeBoardCommands(board: TekdaqcDevice){
        println("Executing Board Commands")
        board.connect(LineNoiseFrequency.AccountFor(60.hertz),NetworkProtocol.TELNET)

        board.wrappedTekdaqc.addListener(object: IMessageListener{
            override fun onDigitalInputDataReceived(wrappedTekdaqc: ATekdaqc?, data: DigitalInputData?) {}

            override fun onDebugMessageReceived(wrappedTekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Debug ${message.toString()}")
            }

            override fun onErrorMessageReceived(wrappedTekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Error ${message.toString()}")
            }

            override fun onAnalogInputDataReceived(wrappedTekdaqc: ATekdaqc?, data: AnalogInputCountData?) {
                println("Analog ${data?.data?.toString()}")
            }

            override fun onCommandDataMessageReceived(wrappedTekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Command ${message.toString()}")
            }

            override fun onStatusMessageReceived(wrappedTekdaqc: ATekdaqc?, message: ABoardMessage?) {
                println("Status ${message.toString()}")
            }

            override fun onDigitalOutputDataReceived(wrappedTekdaqc: ATekdaqc?, data: BooleanArray?) {
            }

        })

        launch(CommonPool) {
            board.analogInputs[0].broadcastChannel.consumeEach {
                println("New Value: $it")
            }
        }

        board.analogInputs[0].activate()

        board.wrappedTekdaqc.sample(100)
    }*/
}