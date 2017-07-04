package com.tenkiv.daqc.hardware

import com.tenkiv.BinaryMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.lib.openNewCoroutineListener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel

class ScBinaryStateSensor(val digitalInput: DigitalInput) : Input<BinaryMeasurement> {

    override val broadcastChannel: ConflatedBroadcastChannel<BinaryMeasurement> = ConflatedBroadcastChannel()

    override val isActivated get() = digitalInput.isActivated

    init {
        digitalInput.currentStateBroadcastChannel.openNewCoroutineListener(CommonPool) {
            processNewMeasurement(it)
        }
    }

    override fun activate() = digitalInput.activate()

    override fun deactivate() = digitalInput.deactivate()
}