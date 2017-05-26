package com.tenkiv.daqc.recording

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.consumeAndReturn
import com.tenkiv.daqc.hardware.definitions.BasicUpdatable
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import tec.uom.se.unit.Units
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.measure.quantity.Time
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 5/15/17.
 */
abstract class Recorder<T: DaqcValue>(val timeToRecord: Time? = null,
                        val recordingObjects: Map<Updatable<T>,String>):
        UpdatableListener<T>,
        BasicUpdatable<DaqcValue.Boolean>() {

    override val openSubChannels: MutableList<SubscriptionReceiveChannel<Updatable<T>>> = ArrayList()

    open fun start(){

        if(timeToRecord != null){
            async(CommonPool){
                delay(timeToRecord.to(Units.SECOND).value.toLong(),TimeUnit.SECONDS)
                stop()
            }
        }

        recordingObjects.keys.forEach { launch(context){ openSubChannels.add(it.broadcastChannel.consumeAndReturn(onDataReceived)) } }

        value = DaqcValue.Boolean(true)
    }

    open fun stop(){
        openSubChannels.forEach { it.close() }
        value = DaqcValue.Boolean(false)
    }
}

