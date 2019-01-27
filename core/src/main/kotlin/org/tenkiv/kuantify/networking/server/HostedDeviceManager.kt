package org.tenkiv.kuantify.networking.server

import kotlinx.coroutines.sync.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.lib.*

internal object HostedDeviceManager {

    private val mutexHostedDevices: MutexValue<MutableMap<String, LocalDevice>> =
        MutexValue(HashMap(), Mutex())

    suspend fun registerDevice(device: LocalDevice) {
        mutexHostedDevices.withLock { hostedDevices ->
            if (hostedDevices[device.uid] == null) {
                hostedDevices += device.uid to device
            }
        }
    }

    suspend fun unregisterDevice(device: LocalDevice) {
        mutexHostedDevices.withLock { hostedDevices ->
            hostedDevices -= device.uid
        }
    }

    @Suppress("NAME_SHADOWING")
    suspend fun receiveMessage(route: List<String>, message: String?) {
        val deviceId = route.first()
        val route = route.drop(1)

        mutexHostedDevices.withLock { hostedDevices ->
            //TODO: Handle message being sent to invalid device.
            hostedDevices[deviceId]?.receiveMessage(route, message)
        }
    }

    suspend fun isDeviceHosted(device: Device): Boolean =
        mutexHostedDevices.withLock { hostedDevices ->
            hostedDevices.containsKey(device.uid)
        }

}