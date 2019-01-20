package org.tenkiv.kuantify.hardware.definitions.device

import kotlinx.coroutines.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.networking.server.*
import kotlin.coroutines.*

interface LocalDevice : Device {

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    fun getNewCommunicator(): HostDeviceCommunicator = HostDeviceCommunicator(this, this)

}

suspend fun LocalDevice.isHosting(): Boolean = withContext(Dispatchers.Daqc) {
    HostedDeviceManager.hostedDevices.containsKey(this@isHosting.uid)
}

suspend fun LocalDevice.startHosting() {
    withContext(Dispatchers.Daqc) {
        HostedDeviceManager.registerDevice(this@startHosting, getNewCommunicator())
    }
}

suspend fun LocalDevice.stopHosting() {
    withContext(Dispatchers.Daqc) {
        HostedDeviceManager.unregisterDevice(this@stopHosting)
    }
}