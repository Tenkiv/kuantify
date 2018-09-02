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

package org.tenkiv.kuantify.networking

import org.tenkiv.kuantify.hardware.definitions.device.Device
import java.net.InetAddress
import javax.measure.Quantity
import javax.measure.quantity.Frequency

/**
 * Sealed class representing the different network protocols which can be used to connect to devices.
 */
sealed class ConnectionProtocol {

    /**
     * Object specifying when the device itself is the host and the [Device] being controlled.
     */
    object Host : ConnectionProtocol()

    /**
     * Object specifying communication with NFC
     * Probably only used for handshaking. Hopefully.
     */
    object Nfc : ConnectionProtocol()

    /**
     * Class specifying communication with Bluetooth
     */
    class Bluetooth() : ConnectionProtocol(){
        //TODO UID and Encryption handshake info.
    }

    /**
     * Object specifying communication with SLIP
     */
    object Slip : ConnectionProtocol()

    /**
     * Class specifying communication with TIA/EIA specified by RS-485 or RS-422
     *
     * @param duplexed Specifies if the line is duplexed.
     * @param isMaster Specifies if the device is a master or a slave.
     */
    class Rs485(val duplexed: Boolean, val isMaster: Boolean) : ConnectionProtocol(){
        //TODO Dynamic address setting methods & values, Master & Slave values, and
    }

    /**
     * Class specifying serial communication specified by RS-232
     */
    class Rs232() : ConnectionProtocol(){
        //TODO Methods for serial interface and possibly line numbers.
    }

    /**
     * Class specifying data packet communication over radio frequencies.
     *
     * @param transmissionFrequency The frequency of the radio signals to be received.
     */
    class PacketRadioService(val transmissionFrequency: Quantity<Frequency>) : ConnectionProtocol() {
        //TODO Methods or values for signal conversion.
    }

}

/**
 * Class defining connections that occur across a traditional network.
 */
sealed class NetworkProtocol : ConnectionProtocol() {
    /**
     * The ipv4 or ipv6 [InetAddress] of the device.
     */
    abstract val inetAddress: InetAddress

    /**
     * Class specifying communication with UDP
     */
    data class Udp(override val inetAddress: InetAddress) : NetworkProtocol()

    /**
     * Class specifying communication with TCP
     */
    data class Tcp(override val inetAddress: InetAddress) : NetworkProtocol()

    /**
     * Class specifying communication with SSH
     */
    data class Ssh(override val inetAddress: InetAddress) : NetworkProtocol()

    /**
     * Class specifying communication with TELNET
     */
    data class Telnet(override val inetAddress: InetAddress) : NetworkProtocol()
}

/**
 * Enum for declaring the describing the sharing status of [Device]s, [Input]s, and [Output]s
 */
enum class SharingStatus {
    /**
     * Nothing is shared
     */
    NONE,

    /**
     * Reading of all channels is allowed.
     */
    READ_ALL,

    /**
     * Reading and Writing of all channels is allowed.
     */
    READ_WRITE_ALL
}

/**
 * The exception thrown when a board cannot communicate via the specified protocol.
 */
class UnsupportedProtocolException : Throwable("Board unable to connect with supplied protocol.")