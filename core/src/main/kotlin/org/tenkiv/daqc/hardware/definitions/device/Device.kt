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

interface Device {

    val inetAddr: InetAddress

    val serialNumber: String

    val temperatureReference: QuantityInput<Temperature>

    /**
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    val isConnected: Boolean

    /**
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var networkProtocol: NetworkProtocol

    /**
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var networkSharingStatus: SharingStatus

    fun connect(lineFrequency: LineNoiseFrequency, protocol: NetworkProtocol? = null)

    fun disconnect(protocol: NetworkProtocol?)

    fun initializeDevice()
}