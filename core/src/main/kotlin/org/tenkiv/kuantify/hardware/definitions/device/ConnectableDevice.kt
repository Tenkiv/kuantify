package org.tenkiv.kuantify.hardware.definitions.device

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

    suspend fun connect()

    suspend fun disconnect()

}