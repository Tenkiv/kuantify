package com.tenkiv.daqc.hardware.inputs

import com.tenkiv.BinaryMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input

class SimpleBinaryStateSensor(val digitalInput: DigitalInput) : Input<BinaryMeasurement> {

    override val broadcastChannel get() = digitalInput.currentStateBroadcastChannel

    override val isActive get() = digitalInput.isActive

    override fun activate() = digitalInput.activate()

    override fun deactivate() = digitalInput.deactivate()
}