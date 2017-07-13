package org.tenkiv.daqc.hardware.inputs

import org.tenkiv.BinaryMeasurement
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.Input

class SimpleBinaryStateSensor(val digitalInput: DigitalInput) : Input<BinaryMeasurement> {

    override val broadcastChannel get() = digitalInput.currentStateBroadcastChannel

    override val isActive get() = digitalInput.isActive

    override fun activate() = digitalInput.activate()

    override fun deactivate() = digitalInput.deactivate()
}