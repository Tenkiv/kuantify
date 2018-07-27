package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import org.tenkiv.daqc.Updatable
import org.tenkiv.daqc.daqcThreadContext

// Locators can be turned into a private val if we decide to add get specific device functions to this class later.
class CombinationLocator(vararg locators: DeviceLocator<*>) : Updatable<LocatorUpdate<*>> {

    private val _broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate<*>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out LocatorUpdate<*>>
        get() = _broadcastChannel

    init {
        locators.forEach { locator ->
            launch(daqcThreadContext) {
                locator.broadcastChannel.consumeEach { device -> _broadcastChannel.offer(device) }
            }
        }
    }

}