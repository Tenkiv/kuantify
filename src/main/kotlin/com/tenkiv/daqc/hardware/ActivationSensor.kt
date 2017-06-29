package com.tenkiv.daqc.hardware

import com.tenkiv.BinaryMeasurement
import com.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel

/**
 * Created by zjuhasz on 6/28/17.
 */
class ActivationSensor(digitalInput: DigitalInput) : Input<BinaryMeasurement> {

    override val broadcastChannel: ConflatedBroadcastChannel<BinaryMeasurement> = ConflatedBroadcastChannel()


}