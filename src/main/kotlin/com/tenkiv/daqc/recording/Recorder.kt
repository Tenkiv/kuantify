package com.tenkiv.daqc.recording

import com.tenkiv.DAQC_CONTEXT
import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.tenkiv.coral.ValueInstant
import tec.uom.se.unit.Units
import java.util.concurrent.TimeUnit
import javax.measure.quantity.Time

abstract class Recorder<T : DaqcValue>(val timeToRecord: Time? = null,
                                       val recordingObjects: Map<ValueInstant<Updatable<T>>, String>) :
        UpdatableListener<T>,
        Updatable<BinaryState> {

    open fun start() {

        if (timeToRecord != null) {
            async(CommonPool) {
                delay(timeToRecord.to(Units.SECOND).value.toLong(), TimeUnit.SECONDS)
                stop()
            }
        }

        recordingObjects.keys.forEach {
            launch(CommonPool) { it.value.broadcastChannel.consumeEach { value -> onUpdate(it.value, value) } }
        }

        launch(DAQC_CONTEXT) { broadcastChannel.send(BinaryState.On) }
    }

    open fun stop() {
        launch(DAQC_CONTEXT) { broadcastChannel.send(BinaryState.Off) }
    }
}

