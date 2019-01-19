package org.tenkiv.kuantify.networking.server

import org.tenkiv.kuantify.hardware.definitions.device.*

// Must only be accessed from daqc dispatcher
internal object HostedDeviceManager {

    val hostedDevices: MutableMap<String, HostDeviceCommunicator> = HashMap()

    fun registerDevice(device: LocalDevice, communicator: HostDeviceCommunicator) {
        if (hostedDevices[device.uid] == null) {
            hostedDevices += device.uid to communicator
        }
    }

    fun unregisterDevice(device: LocalDevice) {
        hostedDevices[device.uid]?.cancel()
        hostedDevices -= device.uid
    }

    @Suppress("NAME_SHADOWING")
    fun receiveMessage(route: List<String>, message: String) {
        val deviceId = route.first()
        val route = route.drop(1)

        //TODO: Handle message being sent to invalid device.
        hostedDevices[deviceId]?.receiveMessage(route, message)
    }

}