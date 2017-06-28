package com.tenkiv.daqc.networking

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlin.reflect.KClass

/**
 * Created by tenkiv on 6/28/17.
 */
class Locator: Updatable<Device> {

    override val broadcastChannel: ConflatedBroadcastChannel<Device> = ConflatedBroadcastChannel()

    private val loctaorList = ArrayList<RemoteLocator<List<Device>>>()

    public fun addDeviceLocator(locator: KClass){

    }


}