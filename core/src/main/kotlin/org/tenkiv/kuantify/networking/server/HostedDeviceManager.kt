package org.tenkiv.kuantify.networking.server

import org.tenkiv.kuantify.hardware.definitions.device.*

// Must only be accessed from daqc dispatcher
internal object HostedDeviceManager {

    val hostedDevices: MutableMap<LocalDevice, HostDeviceCommunicator> = HashMap()

    fun registerDevice(device: LocalDevice, communicator: HostDeviceCommunicator) {
        if (hostedDevices[device] == null) {
            hostedDevices += device to communicator
        }
    }

    fun unregisterDevice(device: LocalDevice) {
        hostedDevices[device]?.cancel()
        hostedDevices -= device
    }

}