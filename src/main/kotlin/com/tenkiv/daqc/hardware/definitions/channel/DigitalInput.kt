package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.BasicUpdatable
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.Updatable
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.util.concurrent.CopyOnWriteArrayList
import javax.measure.quantity.ElectricPotential
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalInput: Input<DaqcValue.Boolean>,
        Channel<DaqcValue.Boolean>,
        BasicUpdatable<DaqcValue.Boolean>() {

    abstract val canReadPulseWidthModulation: Boolean

}