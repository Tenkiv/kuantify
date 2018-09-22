package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.networking.ConnectionHandler
import org.tenkiv.kuantify.networking.SharingStatus

/**
 * Interface defining the basic features of a device that can be connected to. This is in most cases a device located
 * across a network or serial connection.
 */
interface ConnectableDevice : Device {

    /**
     * The [SharingStatus] of the Device showing what channels are available for connection.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var networkSharingStatus: SharingStatus

    /**
     * Value representing if the Device is connected.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    val isConnected: Boolean

    val connectionHandlers: List<ConnectionHandler>

}