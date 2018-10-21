package org.tenkiv.kuantify.networking

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import org.tenkiv.kuantify.hardware.definitions.device.Device

interface ConnectionHandler : CoroutineScope {

    /**
     * The [ConnectionProtocol] which the Device has been discovered over.
     */
    val connectionProtocol: ConnectionProtocol

    var handlerStatus: HandlerConnectionStatus

    /**
     * Function to connect to the [Device].
     *
     * @param protocol The [ConnectionProtocol] to connect over.
     */
    suspend fun connect(): HandlerConnectionStatus

    /**
     * Function to disconnect this [Device].
     *
     * @param The [ConnectionProtocol] to disconnect via.
     */
    suspend fun disconnect(): HandlerConnectionStatus

    fun isValidProtocol(connectionProtocol: ConnectionProtocol): Boolean

}

sealed class HandlerConnectionStatus

data class ConnectedHandler<I, O>(val receiver: ReceiveChannel<O>, val sender: SendChannel<I>) :
    HandlerConnectionStatus()

object UnconnectedHandler : HandlerConnectionStatus()