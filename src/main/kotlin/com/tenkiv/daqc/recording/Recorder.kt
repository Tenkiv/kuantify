package com.tenkiv.daqc.recording

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import tec.uom.se.unit.Units
import java.util.concurrent.TimeUnit
import javax.measure.quantity.Time

/**
 * Created by tenkiv on 5/15/17.
 */
abstract class Recorder<T: DaqcValue>(val timeToRecord: Time? = null,
                        val recordingObjects: Map<Updatable<T>,String>):
                        UpdatableListener<T>,
                        Updatable<DaqcValue.Boolean> {

    open fun start(){

        if(timeToRecord != null){
            async(CommonPool){
                delay(timeToRecord.to(Units.SECOND).value.toLong(),TimeUnit.SECONDS)
                stop()
            }
        }

        recordingObjects.keys.forEach { launch(CommonPool){ it.broadcastChannel.consumeEach{value -> onUpdate(it,value)} } }

        launch(CommonPool){ broadcastChannel.send(DaqcValue.Boolean(true)) }
    }

    open fun stop(){
        launch(CommonPool){ broadcastChannel.send(DaqcValue.Boolean(false)) }
    }
}

