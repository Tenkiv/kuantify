package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.*
import com.tenkiv.daqc.hardware.definitions.channel.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by tenkiv on 4/5/17.
 */
abstract class Controller<T: DaqcValue>(val commands: Map<String,ControllerCommand>): Output<T>{

    override val listeners: MutableList<UpdatableListener<T>> = CopyOnWriteArrayList()

    /*override fun setState(state: T) {
        super.setState(state)
    }*/

    /*override fun setStateByOutputCommand(command: Output.OutputCommand) {
    }

    suspend fun activate(){
        commandList.forEach {
            executeCommand(it)
        }
    }

    suspend fun reverse(){
        val i = commandList.size
        while (i>0){
            executeCommand(commandList[i])
        }
    }

    suspend fun executeCommand(command: ControllerCommand<*>){
        when(command.getTriple().first){
            Output.OutputCommand.SET_VALUE -> {
                if(command is SetAnalogOutputCommand){
                    command.getTriple().third.setState(command.getTriple().second)
                } else if(command is SetDigitalOutputCommand){
                    command.getTriple().third.setState(command.getTriple().second)
                }
            }
            Output.OutputCommand.DELAY -> {
                val seconds = (command.getTriple().second as? DaqcValue.Quantity<*>).to(Units.SECOND)
                delay(seconds.first!!.quantity.getValue().toLong(), TimeUnit.SECONDS)
            }
            Output.OutputCommand.PULSE_WIDTH_MODULATE -> {
                if(command is PWMOutputCommand){
                    command.getTriple().third.setStateByOutputCommand(Output.OutputCommand.PULSE_WIDTH_MODULATE)
                }
            }
        }
    }*/
}

class BasicController(val map: Map<String,ControllerCommand>): Controller<DaqcValue.Boolean>(map){

    var _value: DaqcValue.Boolean? = DaqcValue.Boolean(false)

    override var value: DaqcValue.Boolean?
        get() = _value
        set(value) {_value = value}



}

class ControllerBuilder(){

    private val commandMap = HashMap<String,ControllerCommand>()
    private var counter = 0

    infix fun issue(command: ControllerCommand): ControllerBuilder{
        commandMap.put(counter.toString(),command)
        counter++
        return this
    }

    fun build(): Controller<DaqcValue.Boolean>{
        return BasicController(commandMap)
    }


}