package com.tenkiv.daqc.hardware.definitions

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel

/**
 * Created by tenkiv on 4/11/17.
 */
interface Updatable<T> {

    val broadcastChannel: ConflatedBroadcastChannel<T>

}