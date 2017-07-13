package org.tenkiv.daqc.hardware.outputs

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.hardware.definitions.Updatable
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.hardware.definitions.channel.Output

class SimpleBinaryStateController(val digitalOutput: DigitalOutput,
                                  val inverted: Boolean = false) : Output<BinaryState>, Updatable<BinaryState> {

    override val isActive: Boolean = digitalOutput.isActive

    override fun deactivate() = digitalOutput.deactivate()

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