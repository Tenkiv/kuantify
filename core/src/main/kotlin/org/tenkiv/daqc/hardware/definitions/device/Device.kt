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

package org.tenkiv.daqc.hardware.definitions.device

import org.tenkiv.daqc.gate.acquire.input.QuantityInput
import org.tenkiv.daqc.hardware.LineNoiseFrequency
import org.tenkiv.daqc.networking.NetworkProtocol
import org.tenkiv.daqc.networking.SharingStatus
import java.net.InetAddress
import javax.measure.quantity.Temperature

/**
 * The interface defining the basic aspects of all devices.
 */
interface Device {

    /**
     * The [InetAddress] of the device.
     */
    val inetAddr: InetAddress

    /**
     * The device's serial number. This should be unique.
     */
    val serialNumber: String

    /**
     * The temperature reference of the board for error correction on samples.
     */
    val temperatureReference: QuantityInput<Temperature>

    /**
     * Value representing if the Device is connected.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    val isConnected: Boolean

    /**
     * The [NetworkProtocol] which the Device is connected over.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var networkProtocol: NetworkProtocol

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
     * @param lineFrequency The [LineNoiseFrequency] of the electrical grid the [Device] is physically connected to.
     * @param protocol The [NetworkProtocol] to connect over.
     */
    fun connect(lineFrequency: LineNoiseFrequency, protocol: NetworkProtocol? = null)

    /**
     * Function to disconnect this [Device].
     *
     * @param The [NetworkProtocol] to disconnect via.
     */
    fun disconnect(protocol: NetworkProtocol?)

    /**
     * Function called to initialize the [Device] for first usage if necessary.
     */
    fun initializeDevice()
}