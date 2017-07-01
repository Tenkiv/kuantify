package com.tenkiv.daqc.hardware

import com.tenkiv.BinaryMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel

class BinaryStateSensor(digitalInput: DigitalInput) : Input<BinaryMeasurement> {

    override val broadcastChannel: ConflatedBroadcastChannel<BinaryMeasurement> = ConflatedBroadcastChannel()


}