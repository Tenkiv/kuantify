package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.networking.ConnectionProtocol
import org.tenkiv.kuantify.networking.SharingStatus

/**
 * Interface defining the basic features of a device that can be connected to. This is in most cases a device located
 * across a network or serial connection.
 */
interface ConnectableDevice : Device {

    /**
     * Value representing if the Device is connected.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    val isConnected: Boolean

    /**
     * The [ConnectionProtocol]s which the Device has been discovered over.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var connectionProtocol: List<ConnectionProtocol>

    /**
     * The [SharingStatus] of the Device showing what channels are available for connection.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var networkSharingStatus: SharingStatus

    /**
     * Function to connect to the [Device].
     *
     * @param protocol The [ConnectionProtocol] to connect over.
     */
    fun connect(protocol: ConnectionProtocol)

    /**
     * Function to disconnect this [Device].
     *
     * @param The [ConnectionProtocol] to disconnect via.
     */
    fun disconnect(protocol: ConnectionProtocol)

}