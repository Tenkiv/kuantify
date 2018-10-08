package org.tenkiv.kuantify.networking

import kotlinx.coroutines.experimental.CoroutineScope
import org.tenkiv.kuantify.hardware.definitions.device.Device

interface ConnectionHandler<I, O> : CoroutineScope {

    /**
     * The [ConnectionProtocol] which the Device has been discovered over.
     */
    val connectionProtocol: ConnectionProtocol

    val outputStream: O

    val inputStream: I

    /**
     * Function to connect to the [Device].
     *
     * @param protocol The [ConnectionProtocol] to connect over.
     */
    fun connect()

    /**
     * Function to disconnect this [Device].
     *
     * @param The [ConnectionProtocol] to disconnect via.
     */
    fun disconnect()

    fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean

}