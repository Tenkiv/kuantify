package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.daqc.hardware.definitions.channel.BinaryStateInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput

class SimpleBinaryStateSensor(val digitalInput: DigitalInput) : BinaryStateInput {

    override val broadcastChannel get() = digitalInput.broadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isActive get() = digitalInput.isActive

    override fun activate() = digitalInput.activate()

    override fun deactivate() = digitalInput.deactivate()
}