/*
 * Copyright 2018 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.kuantify.hardware.definitions.device

import org.tenkiv.kuantify.networking.ConnectionProtocol
import org.tenkiv.kuantify.networking.SharingStatus

/**
 * The interface defining the basic aspects of all devices.
 */
interface Device {

    /**
     * The device's serial number. This should be unique.
     */
    val serialNumber: String

    /**
     * Value representing if the Device is connected.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    val isConnected: Boolean

    /**
     * The [ConnectionProtocol] which the Device is connected over.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var connectionProtocol: ConnectionProtocol

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
    fun connect(protocol: ConnectionProtocol? = null)

    /**
     * Function to disconnect this [Device].
     *
     * @param The [ConnectionProtocol] to disconnect via.
     */
    fun disconnect(protocol: ConnectionProtocol?)

    /**
     * Function called to initialize the [Device] for first usage if necessary.
     */
    fun initializeDevice()
}