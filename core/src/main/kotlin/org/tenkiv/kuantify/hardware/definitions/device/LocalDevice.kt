package org.tenkiv.kuantify.hardware.definitions.device

import kotlinx.coroutines.*
import org.tenkiv.kuantify.networking.server.*
import kotlin.coroutines.*

interface LocalDevice : Device {

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    val networkCommunicator: HostDeviceCommunicator

}

suspend fun LocalDevice.isHosting(): Boolean = HostedDeviceManager.isDeviceHosted(this)

suspend fun LocalDevice.startHosting() {
    networkCommunicator.start()
    HostedDeviceManager.registerDevice(this)
}

suspend fun LocalDevice.stopHosting() {
    HostedDeviceManager.unregisterDevice(this)
    networkCommunicator.stop()
}