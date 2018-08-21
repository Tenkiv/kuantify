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

package org.tenkiv.daqc.networking

import org.tenkiv.daqc.Input
import org.tenkiv.daqc.Output
import org.tenkiv.daqc.hardware.definitions.device.Device

/**
 * Enum representing the different network protocols which can be used to connect to devices.
 */
enum class NetworkProtocol {
    /**
     * Communicate with UDP
     */
    UDP,

    /**
     * Communicate with TCP
     */
    TCP,

    /**
     * Communicate with SSH
     */
    SSH,

    /**
     * Communicate with TELNET
     */
    TELNET
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