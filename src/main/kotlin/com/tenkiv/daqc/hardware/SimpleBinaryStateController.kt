package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import com.tenkiv.daqc.hardware.definitions.channel.Output
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel

class SimpleBinaryStateController(val digitalOutput: DigitalOutput,
                                  val inverted: Boolean = false) : Output<BinaryState>, Updatable<BinaryState> {

    override val broadcastChannel: ConflatedBroadcastChannel<BinaryState> = ConflatedBroadcastChannel()

    override fun setOutput(setting: BinaryState) {
        if (!inverted)
            digitalOutput.setOutput(setting)
        else
            when (setting) {
                BinaryState.On -> digitalOutput.setOutput(BinaryState.Off)
                BinaryState.Off -> digitalOutput.setOutput(BinaryState.On)
            }
        broadcastChannel.offer(setting)
    }

}