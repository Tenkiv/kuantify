/*
 * Copyright 2020 Tenkiv, Inc.
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

package kuantify.hardware.device

import kuantify.networking.*
import org.tenkiv.coral.*
import kotlin.time.*

/**
 * Interface defining the basic features of a device that can be connected to. This is in most cases a device located
 * across a network or serial connection.
 */
public interface RemoteDevice : Device {

    /**
     * Value representing if the Device is connected.
     *
     * If you need to take an action upon unexpected connection disruption set up
     * [org.tenkiv.kuantify.handleCriticalDaqcErrors].
     * Otherwise, the only possible way for connection to be lost is by explicitly calling [disconnect].
     */
    public val isConnected: Boolean

    /**
     * Attempts to reconnect using whatever connection method was last used to connect to this device.
     * If this device was never previously connected to, this returns failure.
     */
    public suspend fun reconnect(
        timeout: Duration
    ): Result<Unit, ReconnectError>

    public suspend fun disconnect()

}