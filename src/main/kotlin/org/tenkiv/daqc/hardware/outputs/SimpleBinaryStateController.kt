package org.tenkiv.daqc.hardware.outputs

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.BinaryStateMeasurement
import org.tenkiv.coral.now
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.hardware.definitions.channel.BinaryStateOutput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput

class SimpleBinaryStateController(val digitalOutput: DigitalOutput,
                                  val inverted: Boolean = false) : BinaryStateOutput {

    override val isActive: Boolean = digitalOutput.isActive

    private val _broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    override val broadcastChannel: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _broadcastChannel

    override fun deactivate() = digitalOutput.deactivate()

    override fun setOutput(setting: BinaryState) {
        if (!inverted)
            digitalOutput.setOutput(setting)
        else
            when (setting) {
                BinaryState.On -> digitalOutput.setOutput(BinaryState.Off)
                BinaryState.Off -> digitalOutput.setOutput(BinaryState.On)
            }
        //TODO Change this to broadcast new setting when the board confirms the setting was received.
        _broadcastChannel.offer(setting.now())
    }

}