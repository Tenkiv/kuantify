package org.tenkiv.kuantify.hardware.definitions.device

import kotlinx.coroutines.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.networking.server.*
import kotlin.coroutines.*

interface LocalDevice : Device {

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    fun getNewCommunicator(): HostDeviceCommunicator = DefaultHostDeviceCommunicator(this)

    //TODO
    val isHosting: Boolean

}

fun LocalDevice.startHosting() {
    launch(Dispatchers.Daqc) {
        HostedDeviceManager.registerDevice(this@startHosting, getNewCommunicator())
    }
}

fun LocalDevice.stopHosting() {
    launch(Dispatchers.Daqc) {
        HostedDeviceManager.unregisterDevice(this@stopHosting)
    }
}