package org.tenkiv.kuantify.hardware.definitions.device

import kotlinx.coroutines.*
import org.tenkiv.kuantify.networking.server.*
import kotlin.coroutines.*

interface LocalDevice : Device {

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    fun getNewCommunicator(): HostDeviceCommunicator = HostDeviceCommunicator(this, this)

}

suspend fun LocalDevice.isHosting(): Boolean = HostedDeviceManager.isDeviceHosted(this)

suspend fun LocalDevice.startHosting() {
    HostedDeviceManager.registerDevice(this, getNewCommunicator())
}

suspend fun LocalDevice.stopHosting() {
    HostedDeviceManager.unregisterDevice(this)
}