package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.daqc.hardware.definitions.channel.BinaryStateInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput

class SimpleBinaryStateSensor(val digitalInput: DigitalInput) : BinaryStateInput {

    override val broadcastChannel get() = digitalInput.currentStateBroadcastChannel

    override val isActive get() = digitalInput.isActive

    override fun activate() = digitalInput.activate()

    override fun deactivate() = digitalInput.deactivate()
}