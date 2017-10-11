package org.tenkiv.daqc.hardware.inputs

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.at
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.BinaryStateMeasurement
import org.tenkiv.daqc.hardware.definitions.BinaryStateInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.lib.openNewCoroutineListener

class SimpleBinaryStateSensor internal constructor(val digitalInput: DigitalInput) : BinaryStateInput {

    @Volatile
    var inverted: Boolean = false

    private val _broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    override val broadcastChannel: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _broadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForBinaryState

    init {
        digitalInput.broadcastChannel.openNewCoroutineListener(CommonPool) { measurement ->
            if (!inverted)
                _broadcastChannel.send(measurement)
            else
                when (measurement.value) {
                    BinaryState.On -> _broadcastChannel.send(BinaryState.Off at measurement.instant)
                    BinaryState.Off -> _broadcastChannel.send(BinaryState.On at measurement.instant)
                }
        }
    }

    override fun activate() = digitalInput.activateForCurrentState()

    override fun deactivate() = digitalInput.deactivate()
}